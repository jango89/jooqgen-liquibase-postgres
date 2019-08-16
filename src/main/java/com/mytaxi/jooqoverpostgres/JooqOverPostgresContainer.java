package com.mytaxi.jooqoverpostgres;

import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.resource.FileSystemResourceAccessor;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.jooq.codegen.GenerationTool;
import org.jooq.meta.jaxb.Configuration;
import org.jooq.meta.jaxb.Generate;
import org.jooq.meta.jaxb.Generator;
import org.jooq.meta.jaxb.Jdbc;
import org.jooq.meta.jaxb.Target;
import org.junit.ClassRule;
import org.testcontainers.containers.PostgreSQLContainer;

@Mojo(name = "jooqOverPostgresContainer")
public class JooqOverPostgresContainer extends AbstractMojo
{

    public static final String TARGET_GENERATED_SOURCES_JOOQ = "target/generated-sources/jooq/";
    @ClassRule
    public static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:alpine");

    @Parameter(property = "jooqOverPostgresContainer.packageName", required = true)
    private String packageName;

    @Parameter(property = "jooqOverPostgresContainer.schema", required = true)
    private String schema;

    @Parameter(property = "jooqOverPostgresContainer.liquibaseChangeLogFile", required = true)
    private String liquibaseChangeLogFile;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;


    @Override
    public void execute()
    {
        postgreSQLContainer.
            withDatabaseName(schema)
            .start();

        try (Connection connection = getConnection())
        {
            liquibase(connection);
            GenerationTool.generate(getConfiguration());
            project.addCompileSourceRoot(TARGET_GENERATED_SOURCES_JOOQ);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        finally
        {
            postgreSQLContainer.close();
        }
    }


    private Configuration getConfiguration()
    {
        return new Configuration()
            .withJdbc(getJdbc(schema))
            .withGenerator(new Generator()
                .withDatabase(getDatabaseConfigForJooq())
                .withGenerate(generate())
                .withTarget(getJooqTarget(packageName)));
    }


    private Generate generate()
    {
        return new Generate()
            .withPojos(true)
            .withRecords(true)
            .withDaos(true);
    }


    private Jdbc getJdbc(String schema)
    {
        return new Jdbc()
            .withDriver("org.postgresql.Driver")
            .withUrl(postgreSQLContainer.getJdbcUrl())
            .withUser(postgreSQLContainer.getUsername())
            .withSchema(schema)
            .withPassword(postgreSQLContainer.getPassword());
    }


    private Target getJooqTarget(String targetPackage)
    {
        return new Target()
            .withPackageName(targetPackage)
            .withDirectory(Paths.get(TARGET_GENERATED_SOURCES_JOOQ).toAbsolutePath().toString());
    }


    private org.jooq.meta.jaxb.Database getDatabaseConfigForJooq()
    {
        return new org.jooq.meta.jaxb.Database()
            .withName("org.jooq.meta.postgres.PostgresDatabase")
            .withIncludes(".*")
            .withExcludes("databasechangelog.*")
            .withOutputSchemaToDefault(true)
            .withInputSchema("public");
    }


    private void liquibase(Connection connection) throws LiquibaseException
    {
        new Liquibase(Paths.get(liquibaseChangeLogFile).toAbsolutePath().toString(), new FileSystemResourceAccessor(), getDatabase(connection))
            .update("main");
    }


    private Database getDatabase(Connection c) throws DatabaseException
    {
        return DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(c));
    }


    private Connection getConnection() throws SQLException
    {
        return DriverManager.getConnection(postgreSQLContainer.getJdbcUrl(), postgreSQLContainer.getUsername(), postgreSQLContainer.getPassword());
    }

}
