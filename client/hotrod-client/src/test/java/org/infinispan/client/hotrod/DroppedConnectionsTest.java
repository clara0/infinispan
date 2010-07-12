package org.infinispan.client.hotrod;

import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.infinispan.client.hotrod.impl.transport.tcp.TcpTransport;
import org.infinispan.client.hotrod.impl.transport.tcp.TcpTransportFactory;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.test.SingleCacheManagerTest;
import org.infinispan.test.TestingUtil;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import java.net.InetSocketAddress;
import java.util.Properties;

import static org.testng.AssertJUnit.assertEquals;

/**
 * @author Mircea.Markus@jboss.com
 * @since 4.1
 */
@Test (testName = "client.hotrod.DroppedConnectionsTest", groups = "functional")
public class DroppedConnectionsTest extends SingleCacheManagerTest {
   private HotRodServer hotRodServer;
   private RemoteCacheManager remoteCacheManager;
   private RemoteCache rc;
   private TcpTransportFactory transportFactory;

   @Override
   protected EmbeddedCacheManager createCacheManager() throws Exception {
      cacheManager = TestCacheManagerFactory.createCacheManager(getDefaultStandaloneConfig(true));
      hotRodServer = TestHelper.startHotRodServer(cacheManager);
      Properties hrClientConfig = new Properties();
      hrClientConfig.put("testWhileIdle", "false");
      hrClientConfig.put("minIdle","1");
      hrClientConfig.put("maxIdle","2");
      hrClientConfig.put("maxActive","2");
      hrClientConfig.put("hotrod-servers", "127.0.0.1:" + hotRodServer.getPort());
      remoteCacheManager = new RemoteCacheManager(hrClientConfig);
      rc = remoteCacheManager.getCache();
      transportFactory = (TcpTransportFactory) TestingUtil.extractField(remoteCacheManager, "transportFactory");
      return cacheManager;
   }

   @AfterClass
   public void shutDownHotrod() {
      remoteCacheManager.stop();
      hotRodServer.stop();
   }

   public void closedConnectionTest() throws Exception {
      rc.put("k","v"); //make sure a connection is created

      GenericKeyedObjectPool keyedObjectPool = transportFactory.getConnectionPool();
      InetSocketAddress address = new InetSocketAddress("127.0.0.1", hotRodServer.getPort());

      assertEquals(0, keyedObjectPool.getNumActive(address));
      assertEquals(1, keyedObjectPool.getNumIdle(address));

      TcpTransport tcpConnection = (TcpTransport) keyedObjectPool.borrowObject(address);
      keyedObjectPool.returnObject(address, tcpConnection);//now we have a reference to the single connection in pool

      tcpConnection.destroy();

      assertEquals("v", rc.get("k"));
      assertEquals(0, keyedObjectPool.getNumActive(address));
      assertEquals(1, keyedObjectPool.getNumIdle(address));

      TcpTransport tcpConnection2 = (TcpTransport) keyedObjectPool.borrowObject(address);

      assert tcpConnection2.getId() != tcpConnection.getId();
   }
   
}
