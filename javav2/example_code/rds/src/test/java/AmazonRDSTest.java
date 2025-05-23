// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

import com.example.rds.*;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.services.rds.RdsClient;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import java.io.*;
import java.util.*;

/**
 * To run these integration tests, you must set the required values
 * in the config.properties file or AWS Secrets Manager.
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AmazonRDSTest {
    private static final Logger logger = LoggerFactory.getLogger(AmazonRDSTest.class);
    private static RdsClient rdsClient;

    private static String dbInstanceIdentifier = "";
    private static String dbSnapshotIdentifier = "";
    private static String dbName = "";

    private static String newMasterUserPassword = "";

    // Set data members required for the Scenario test.
    private static String dbGroupNameSc = "";
    private static String dbParameterGroupFamilySc = "";
    private static String dbInstanceIdentifierSc = "";
    private static String secretDBName = "";
    private static String dbSnapshotIdentifierSc = "";
    private static String dbNameSc = "";

    private static String dbClusterGroupName;

    private static String dbParameterGroupFamily;

    private static String dbInstanceClusterIdentifier;

    @BeforeAll
    public static void setUp() throws IOException {
        rdsClient = RdsClient.builder()
            .region(Region.US_WEST_2)
            .build();

        Random rand = new Random();
        int randomNum = rand.nextInt((10000 - 1) + 1) + 1;

        // Get the values to run these tests from AWS Secrets Manager.
        Gson gson = new Gson();
        String json = getSecretValues();
        SecretValues values = gson.fromJson(json, SecretValues.class);
        dbInstanceIdentifier = values.getDbInstanceIdentifier() + java.util.UUID.randomUUID();
        dbSnapshotIdentifier = values.getDbSnapshotIdentifier() + java.util.UUID.randomUUID();
        dbName = values.getDbName() + randomNum;
        newMasterUserPassword = values.getNewMasterUserPassword();
        dbGroupNameSc = values.getDbGroupNameSc() + java.util.UUID.randomUUID();
        dbParameterGroupFamilySc = values.getDbParameterGroupFamilySc();
        dbInstanceIdentifierSc = values.getDbInstanceIdentifierSc() + java.util.UUID.randomUUID();
        dbSnapshotIdentifierSc = values.getDbSnapshotIdentifierSc() + java.util.UUID.randomUUID();
        dbNameSc = values.getDbNameSc() + randomNum;
        dbClusterGroupName = values.getDbClusterGroupName() + randomNum;
        dbParameterGroupFamily = values.getDbParameterGroupFamily();
        dbInstanceClusterIdentifier = values.getDbInstanceClusterIdentifier();
        secretDBName = values.getSecretName();
    }

    @Test
    @Tag("IntegrationTest")
    @Order(1)
    public void testCreateDBInstance() {
        Gson gson = new Gson();
        User user = gson.fromJson(String.valueOf(RDSScenario.getSecretValues(secretDBName)), User.class);
        assertDoesNotThrow(() -> CreateDBInstance.createDatabaseInstance(rdsClient, dbInstanceIdentifier, dbName,
                user.getUsername(), user.getPassword()));
        logger.info("\nTest 1 passed");
    }

    @Test
    @Tag("IntegrationTest")
    @Order(2)
    public void testWaitForInstanceReady() {
        assertDoesNotThrow(() -> CreateDBInstance.waitForInstanceReady(rdsClient, dbInstanceIdentifier));
        logger.info("\nTest 2 passed");
    }

    @Test
    @Tag("IntegrationTest")
    @Order(3)
    public void testDescribeAccountAttributes() {
        assertDoesNotThrow(() -> DescribeAccountAttributes.getAccountAttributes(rdsClient));
        logger.info("\nTest 3 passed");
    }

    @Test
    @Tag("IntegrationTest")
    @Order(4)
    public void testDescribeDBInstances() {
        assertDoesNotThrow(() -> DescribeDBInstances.describeInstances(rdsClient));
        logger.info("\nTest 4 passed");
    }

    @Test
    @Tag("IntegrationTest")
    @Order(5)
    public void testModifyDBInstance() {
        assertDoesNotThrow(
                () -> ModifyDBInstance.updateIntance(rdsClient, dbInstanceIdentifier, newMasterUserPassword));
        logger.info("\nTest 5 passed");
    }

    @Test
    @Order(6)
    public void testCreateDBSnapshot() {
        assertDoesNotThrow(
                () -> CreateDBSnapshot.createSnapshot(rdsClient, dbInstanceIdentifier, dbSnapshotIdentifier));
        logger.info("\nTest 6 passed");
    }

    @Test
    @Tag("IntegrationTest")
    @Order(7)
    public void testDeleteDBInstance() {
        assertDoesNotThrow(() -> DeleteDBInstance.deleteDatabaseInstance(rdsClient, dbInstanceIdentifier));
        logger.info("\nTest 7 passed");
    }

    @Test
    @Tag("IntegrationTest")
    @Order(8)
    public void testRDSScenario() {
        Gson gson = new Gson();
        User user = gson.fromJson(String.valueOf(RDSScenario.getSecretValues(secretDBName)), User.class);
        assertDoesNotThrow(() -> RDSScenario.describeDBEngines(rdsClient));
        assertDoesNotThrow(
                () -> RDSScenario.createDBParameterGroup(rdsClient, dbGroupNameSc, dbParameterGroupFamilySc));
        assertDoesNotThrow(() -> RDSScenario.describeDbParameterGroups(rdsClient, dbGroupNameSc));
        assertDoesNotThrow(() -> RDSScenario.describeDbParameters(rdsClient, dbGroupNameSc, 0));
        assertDoesNotThrow(() -> RDSScenario.modifyDBParas(rdsClient, dbGroupNameSc));
        assertDoesNotThrow(() -> RDSScenario.describeDbParameters(rdsClient, dbGroupNameSc, -1));
        assertDoesNotThrow(() -> RDSScenario.getAllowedEngines(rdsClient, dbParameterGroupFamilySc));
        assertDoesNotThrow(() -> RDSScenario.getMicroInstances(rdsClient));
        String dbARN = RDSScenario.createDatabaseInstance(rdsClient, dbGroupNameSc, dbInstanceIdentifierSc, dbNameSc,
                user.getUsername(), user.getPassword());
        assertFalse(dbARN.isEmpty());
        assertDoesNotThrow(() -> RDSScenario.waitForInstanceReady(rdsClient, dbInstanceIdentifierSc));
        assertDoesNotThrow(() -> RDSScenario.createSnapshot(rdsClient, dbInstanceIdentifierSc, dbSnapshotIdentifierSc));
        assertDoesNotThrow(
                () -> RDSScenario.waitForSnapshotReady(rdsClient, dbInstanceIdentifierSc, dbSnapshotIdentifierSc));
        assertDoesNotThrow(() -> RDSScenario.deleteDatabaseInstance(rdsClient, dbInstanceIdentifierSc));
        assertDoesNotThrow(() -> RDSScenario.deleteParaGroup(rdsClient, dbGroupNameSc, dbARN));
        logger.info("\nTest 8 passed");
    }

    @Test
    @Tag("IntegrationTest")
    @Order(9)
    public void testAuroraScenario() throws InterruptedException {
        Gson gson = new Gson();
        User user = gson.fromJson(String.valueOf(RDSScenario.getSecretValues(secretDBName)), User.class);
        System.out.println("1. Return a list of the available DB engines");
        assertDoesNotThrow(() -> AuroraScenario.describeDBEngines(rdsClient));
        System.out.println("2. Create a custom parameter group");
        assertDoesNotThrow(() -> AuroraScenario.createDBClusterParameterGroup(rdsClient, dbClusterGroupName,
                dbParameterGroupFamily));
        System.out.println("3. Get the parameter group");
        assertDoesNotThrow(() -> AuroraScenario.describeDbClusterParameterGroups(rdsClient, dbClusterGroupName));
        System.out.println("4. Get the parameters in the group");
        assertDoesNotThrow(() -> AuroraScenario.describeDbClusterParameters(rdsClient, dbClusterGroupName, 0));
        System.out.println("5. Modify the auto_increment_offset parameter");
        assertDoesNotThrow(() -> AuroraScenario.modifyDBClusterParas(rdsClient, dbClusterGroupName));
        System.out.println("6. Display the updated parameter value");
        assertDoesNotThrow(() -> AuroraScenario.describeDbClusterParameters(rdsClient, dbClusterGroupName, -1));
        System.out.println("7. Get a list of allowed engine versions");
        assertDoesNotThrow(() -> AuroraScenario.getAllowedEngines(rdsClient, dbParameterGroupFamily));
        System.out.println("8. Create an Aurora DB cluster database");
        String arnClusterVal = AuroraScenario.createDBCluster(rdsClient, dbClusterGroupName, dbName,
                dbInstanceClusterIdentifier, user.getUsername(), user.getPassword());
        System.out.println("The ARN of the cluster is " + arnClusterVal);
        System.out.println("9. Wait for DB instance to be ready");
        assertDoesNotThrow(() -> AuroraScenario.waitForInstanceReady(rdsClient, dbInstanceClusterIdentifier));
        System.out.println("10. Get a list of instance classes available for the selected engine");
        String instanceClass = AuroraScenario.getListInstanceClasses(rdsClient);
        System.out.println("11. Create a database instance in the cluster.");
        String clusterDBARN = AuroraScenario.createDBInstanceCluster(rdsClient, dbInstanceIdentifier,
                dbInstanceClusterIdentifier, instanceClass);
        System.out.println("The ARN of the database is " + clusterDBARN);
        System.out.println("12. Wait for DB instance to be ready");
        assertDoesNotThrow(() -> AuroraScenario.waitDBInstanceReady(rdsClient, dbInstanceIdentifier));
        System.out.println("13. Create a snapshot");
        assertDoesNotThrow(() -> AuroraScenario.createDBClusterSnapshot(rdsClient, dbInstanceClusterIdentifier,
                dbSnapshotIdentifier));
        System.out.println("14. Wait for DB snapshot to be ready");
        assertDoesNotThrow(() -> AuroraScenario.waitForSnapshotReady(rdsClient, dbSnapshotIdentifier,
                dbInstanceClusterIdentifier));
        System.out.println("14. Delete the DB instance");
        assertDoesNotThrow(() -> AuroraScenario.deleteDatabaseInstance(rdsClient, dbInstanceIdentifier));
        System.out.println("15. Delete the DB cluster");
        assertDoesNotThrow(() -> AuroraScenario.deleteCluster(rdsClient, dbInstanceClusterIdentifier));
        System.out.println("16. Delete the DB cluster group");
        assertDoesNotThrow(() -> AuroraScenario.deleteDBClusterGroup(rdsClient, dbClusterGroupName, clusterDBARN));
        logger.info("\nTest 9 passed");
    }

    private static String getSecretValues() {
        SecretsManagerClient secretClient = SecretsManagerClient.builder()
            .region(Region.US_EAST_1)
            .build();
        String secretName = "test/rds";
        GetSecretValueRequest valueRequest = GetSecretValueRequest.builder()
                .secretId(secretName)
                .build();

        GetSecretValueResponse valueResponse = secretClient.getSecretValue(valueRequest);
        return valueResponse.secretString();
    }

    @Nested
    @DisplayName("A class used to get test values from test/rds (an AWS Secrets Manager secret)")
    class SecretValues {
        private String dbInstanceIdentifier;
        private String dbSnapshotIdentifier;
        private String dbName;

        private String masterUsername;

        private String masterUserPassword;

        private String newMasterUserPassword;

        private String dbGroupNameSc;

        private String dbParameterGroupFamilySc;

        private String dbInstanceIdentifierSc;

        private String dbNameSc;

        private String masterUsernameSc;

        private String masterUserPasswordSc;

        private String dbSnapshotIdentifierSc;

        private String dbClusterGroupName;

        private String dbParameterGroupFamily;

        private String dbInstanceClusterIdentifier;

        private String secretName;

        public String getSecretName() {
            return secretName;
        }

        public String getDbInstanceClusterIdentifier() {
            return dbInstanceClusterIdentifier;
        }

        public String getDbParameterGroupFamily() {
            return dbParameterGroupFamily;
        }

        public String getDbClusterGroupName() {
            return dbClusterGroupName;
        }

        public String getDbInstanceIdentifier() {
            return dbInstanceIdentifier;
        }

        public String getDbSnapshotIdentifier() {
            return dbSnapshotIdentifier;
        }

        public String getDbName() {
            return dbName;
        }

        public String getMasterUsername() {
            return masterUsername;
        }

        public String getMasterUserPassword() {
            return masterUserPassword;
        }

        public String getNewMasterUserPassword() {
            return newMasterUserPassword;
        }

        public String getDbGroupNameSc() {
            return dbGroupNameSc;
        }

        public String getDbParameterGroupFamilySc() {
            return dbParameterGroupFamilySc;
        }

        public String getDbInstanceIdentifierSc() {
            return dbInstanceIdentifierSc;
        }

        public String getDbNameSc() {
            return dbNameSc;
        }

        public String getMasterUsernameSc() {
            return masterUsernameSc;
        }

        public String getMasterUserPasswordSc() {
            return masterUserPasswordSc;
        }

        public String getDbSnapshotIdentifierSc() {
            return dbSnapshotIdentifierSc;
        }
    }
}
