/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.seatunnel.e2e.connector.influxdb;

import org.apache.seatunnel.api.table.type.BasicType;
import org.apache.seatunnel.api.table.type.SeaTunnelDataType;
import org.apache.seatunnel.api.table.type.SeaTunnelRow;
import org.apache.seatunnel.api.table.type.SeaTunnelRowType;
import org.apache.seatunnel.connectors.seatunnel.influxdb.client.InfluxDBClient;
import org.apache.seatunnel.connectors.seatunnel.influxdb.config.InfluxDBConfig;
import org.apache.seatunnel.e2e.common.TestResource;
import org.apache.seatunnel.e2e.common.TestSuiteBase;
import org.apache.seatunnel.e2e.common.container.TestContainer;

import lombok.extern.slf4j.Slf4j;
import org.influxdb.InfluxDB;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestTemplate;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.DockerLoggerFactory;

import java.io.IOException;
import java.net.ConnectException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import scala.Tuple2;

@Slf4j
public class InfluxdbIT extends TestSuiteBase implements TestResource {
    private static final String IMAGE = "influxdb:1.8";
    private static final String HOST = "influxdb-host";
    private static final int PORT = 8764;
    private static final String INFLUXDB_DATABASE = "test";
    private static final String INFLUXDB_MEASUREMENT = "test";

    private static final String INFLUXDB_HOST = "localhost";

    private static final int INFLUXDB_PORT = 8764;

    private static final Tuple2<SeaTunnelRowType, List<SeaTunnelRow>> TEST_DATASET = generateTestDataSet();

    private static final String INFLUXDB_CONNECT_URL = String.format("http://%s:%s", INFLUXDB_HOST, INFLUXDB_PORT);

    private GenericContainer<?> influxdbContainer;

    private InfluxDB influxDB;

    @BeforeAll
    @Override
    public void startUp() throws Exception {
        this.influxdbContainer = new GenericContainer<>(DockerImageName.parse(IMAGE))
            .withNetwork(NETWORK)
            .withNetworkAliases(HOST)
            .withExposedPorts(PORT)
            .withLogConsumer(new Slf4jLogConsumer(DockerLoggerFactory.getLogger(IMAGE)))
            .waitingFor(new HostPortWaitStrategy()
                .withStartupTimeout(Duration.ofMinutes(2)));
        Startables.deepStart(Stream.of(influxdbContainer)).join();
        log.info("Influxdb container started");
        this.initializeInfluxDBClient();
        this.initSourceData();
    }

    private void initSourceData() {
        influxDB.createDatabase(INFLUXDB_DATABASE);
        BatchPoints batchPoints = BatchPoints
                .database(INFLUXDB_DATABASE)
                .build();
        List<SeaTunnelRow> rows = TEST_DATASET._2();
        SeaTunnelRowType rowType = TEST_DATASET._1();

        for (int i = 0; i < rows.size(); i++) {
            SeaTunnelRow row = rows.get(i);
            Point point = Point.measurement(INFLUXDB_MEASUREMENT)
                    .time((Long) row.getField(0), TimeUnit.NANOSECONDS)
                    .tag(rowType.getFieldName(1), (String) row.getField(1))
                    .addField(rowType.getFieldName(2), (String) row.getField(2))
                    .addField(rowType.getFieldName(3), (Double) row.getField(3))
                    .addField(rowType.getFieldName(4), (Long) row.getField(4))
                    .addField(rowType.getFieldName(5), (Float) row.getField(5))
                    .addField(rowType.getFieldName(6), (Integer) row.getField(6))
                    .addField(rowType.getFieldName(7), (Short) row.getField(7))
                    .addField(rowType.getFieldName(8), (Boolean) row.getField(8))
                    .build();
            batchPoints.point(point);
        }
        influxDB.write(batchPoints);
    }

    private static Tuple2<SeaTunnelRowType, List<SeaTunnelRow>> generateTestDataSet() {
        SeaTunnelRowType rowType = new SeaTunnelRowType(
            new String[]{
                "time",
                "label",
                "c_string",
                "c_double",
                "c_bigint",
                "c_float",
                "c_int",
                "c_smallint",
                "c_boolean"
            },
            new SeaTunnelDataType[]{
                BasicType.LONG_TYPE,
                BasicType.STRING_TYPE,
                BasicType.STRING_TYPE,
                BasicType.DOUBLE_TYPE,
                BasicType.LONG_TYPE,
                BasicType.FLOAT_TYPE,
                BasicType.INT_TYPE,
                BasicType.SHORT_TYPE,
                BasicType.BOOLEAN_TYPE
            }
        );

        List<SeaTunnelRow> rows = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            SeaTunnelRow row = new SeaTunnelRow(
                new Object[]{
                    new Date().getTime(),
                    String.format("label_%s", i),
                    String.format("f1_%s", i),
                    Double.parseDouble("1.1"),
                    Long.parseLong("1"),
                    Float.parseFloat("1.1"),
                    Short.parseShort("1"),
                    Integer.valueOf(i),
                    i % 2 == 0 ? Boolean.TRUE : Boolean.FALSE
                });
            rows.add(row);
        }
        return Tuple2.apply(rowType, rows);
    }

    @AfterAll
    @Override
    public void tearDown() throws Exception {
        influxDB.close();
        influxdbContainer.stop();
    }

    @TestTemplate
    public void testRedis(TestContainer container) throws IOException, InterruptedException {
        Container.ExecResult execResult = container.executeJob("/influxdb-to-influxdb.conf");
        Assertions.assertEquals(0, execResult.getExitCode());
    }

    private void initializeInfluxDBClient() throws ConnectException {
        InfluxDBConfig influxDBConfig = new InfluxDBConfig(INFLUXDB_CONNECT_URL);
        influxDB = InfluxDBClient.getInfluxDB(influxDBConfig);
    }
}
