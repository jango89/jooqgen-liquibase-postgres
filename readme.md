# What is this

Maven plugin which can be integrated to any maven project
Sample :   
```   
<plugin>  
    <groupId>com.mytaxi</groupId>  
     <artifactId>jooqgen-liquibase-postgres</artifactId>
    <configuration>
        <schema>bookingoptionsservice</schema> <-- schema name -->
        <packageName>com.mytaxi.bookingoptionsservice</packageName> <-- package to be created for generated classes -->
        <liquibaseChangeLogFile>${liquibase.changeLogFile}</liquibaseChangeLogFile> 
    </configuration>
    <executions>
         <execution>
             <phase>generate-sources</phase>
            <goals>
                 <goal>jooqOverPostgresContainer</goal>
            </goals>
        </execution>
    </executions>
 </plugin>      
 ``` 

# What it does

1. Starts a postgress docker container.
2. Applies liquibase changes over the container.
3. Generates JOOQ classes for the source project connecting to postgres container.


# Problems and solutions

If generated classes fail to compile, 
1. include ` /target/generated-sources/jooq/` folder to corresponding compiler plugin.