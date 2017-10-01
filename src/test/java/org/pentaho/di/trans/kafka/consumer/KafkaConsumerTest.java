package org.pentaho.di.trans.kafka.consumer;

import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ZookeeperConsumerConnector;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.pentaho.di.core.KettleEnvironment;
import org.pentaho.di.core.RowMetaAndData;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.variables.Variables;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.TransTestFactory;
import org.pentaho.di.trans.step.StepMeta;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

@PowerMockIgnore("javax.management.*")
@RunWith(PowerMockRunner.class)
@PrepareForTest({Consumer.class})
public class KafkaConsumerTest {

    static final String STEP_NAME = "Kafka Step";

    @Mock
    Map<String, List<KafkaStream<byte[], byte[]>>> streamsMap;
    @Mock
    KafkaStream<byte[], byte[]> kafkaStream;
    @Mock
    ZookeeperConsumerConnector zookeeperConsumerConnector;
    @Mock
    ConsumerIterator<byte[], byte[]> streamIterator;
    @Mock
    List<KafkaStream<byte[], byte[]>> stream;

    private KafkaConsumer step;
    private StepMeta stepMeta;
    private KafkaConsumerMeta meta;
    private KafkaConsumerData data;
    private TransMeta transMeta;
    private Trans trans;

    @BeforeClass
    public static void setUpBeforeClass() throws KettleException {
        KettleEnvironment.init(false);
    }

    @Before
    public void setUp() {
        data = new KafkaConsumerData();
        meta = new KafkaConsumerMeta();
        meta.setKafkaProperties(getDefaultKafkaProperties());

        stepMeta = new StepMeta("KafkaConsumer", meta);
        transMeta = new TransMeta();
        transMeta.addStep(stepMeta);
        trans = new Trans(transMeta);

        PowerMockito.mockStatic(Consumer.class);

        when(Consumer.createJavaConsumerConnector(any(ConsumerConfig.class))).thenReturn(zookeeperConsumerConnector);
        when(zookeeperConsumerConnector.createMessageStreams(anyMapOf(String.class, Integer.class))).thenReturn(streamsMap);
        when(streamsMap.get(anyString())).thenReturn(stream);
        when(stream.get(anyInt())).thenReturn(kafkaStream);
        when(kafkaStream.iterator()).thenReturn(streamIterator);
    }

    @Test(expected = IllegalArgumentException.class)
    public void stepInitConfigIssue() throws Exception {
        step = new KafkaConsumer(stepMeta, data, 1, transMeta, trans);
        meta.setKafkaProperties(new Properties());

        step.init(meta, data);
    }

    // If the step does not receive any rows, the transformation should still run successfully
    @Test
    public void testNoInput() throws KettleException {
        TransMeta tm = TransTestFactory.generateTestTransformation(new Variables(), meta, STEP_NAME);

        List<RowMetaAndData> result = TransTestFactory.executeTestTransformation(tm, TransTestFactory.INJECTOR_STEPNAME,
                STEP_NAME, TransTestFactory.DUMMY_STEPNAME, new ArrayList<RowMetaAndData>());

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    private Properties getDefaultKafkaProperties() {
        Properties p = new Properties();
        p.put("zookeeper.connect", "");
        p.put("group.id", "");

        return p;
    }
}
