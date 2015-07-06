/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.hudson.test.integration.subscribe;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.ArtifactStoreBuilder;
import de.flapdoodle.embed.mongo.config.DownloadConfigBuilder;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.config.RuntimeConfigBuilder;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.extract.UserTempNaming;
import de.flapdoodle.embed.process.runtime.Network;
import org.jenkinsci.plugins.compatibilityaction.CompatibilityDataProvider;
import org.jenkinsci.plugins.compatibilityaction.MongoProviderImpl;
import org.junit.rules.ExternalResource;

/**
 *
 * @author Mads
 */
public class MongoExternalDataSourceRule extends ExternalResource {

    private static final int PORT = 12345; 
    private static final String HOST = "localhost";
    private static final String COLLECTION = "test_collection";
    private static final String DBNAME = "test";
    private static final MongodStarter starter;
    
    static {
        starter = MongodStarter.getInstance(new RuntimeConfigBuilder()
                .defaults(Command.MongoD)
                .artifactStore(new ArtifactStoreBuilder()
                    .defaults(Command.MongoD)
                    .download(new DownloadConfigBuilder()
                    .defaultsForCommand(Command.MongoD))
                    .executableNaming(new UserTempNaming()))
                .build());
    }
    
    private MongodExecutable _mongodExe;
    private MongodProcess _mongod;
    private MongoClient _mongo;
    
    public CompatibilityDataProvider getProvider() {
        return new MongoProviderImpl(HOST, PORT, DBNAME, COLLECTION, null, null);
    }
    
    @Override
    protected void after() {
        _mongod.stop();   
        _mongodExe.stop();
    }

    @Override
    protected void before() throws Throwable {
        int port = 12345;
        IMongodConfig mongodConfig;
        mongodConfig = new MongodConfigBuilder()
                .version(Version.Main.PRODUCTION)
                .net(new Net(port, Network.localhostIsIPv6()))                    
                .build();
        _mongodExe = starter.prepare(mongodConfig);

        _mongod = _mongodExe.start();
        _mongo = new MongoClient(HOST, PORT);

        DB db = _mongo.getDB(DBNAME);
        DBCollection col = db.createCollection(COLLECTION, new BasicDBObject());    
    }
}
