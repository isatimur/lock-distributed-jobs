package services.job;

import akka.actor.ActorSystem;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import org.springframework.util.Base64Utils;
import scala.concurrent.ExecutionContext;
import services.Counter;
import utilities.Constants;
import utilities.EtcdLock;

import java.text.ParseException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * ${CLASS_NAME}. Created at 4/2/2018 10:16 AM by
 * @author Timur Isachenko
 */
public class ArchiveOldEventsJob extends AbstractAkkaJob {

    private final Counter counter;
    private final EtcdLock etcdLock;
    private final String etcdLockName;

    @Inject
    public ArchiveOldEventsJob(final ActorSystem actorSystem, final ExecutionContext executionContext, final Config config, final Counter counter, final EtcdLock etcdLock) {
        super(actorSystem, executionContext, config);
        this.counter = counter;
        this.etcdLock = etcdLock;
        this.etcdLockName = config.getString(Constants.ETCD_LOCK_NAME);
    }

    @Inject
    public void init() throws ParseException {
        try {
            logger.debug(">> init()");
            start(Constants.ARCHIVE_OLD_EVENTS_JOB_CRON);
        } finally {
            logger.debug("<< init()");
        }
    }

    @Override
    protected void process() {
        logger.info(">>>> Start Job");
        try {
            Optional<String> key = Optional.ofNullable(etcdLock.aquireLock(Base64Utils.encode(etcdLockName.getBytes())).toCompletableFuture().get());
            if (key.isPresent()) {
                Thread.sleep(10000);
                int count = counter.nextCount();
                logger.info(">>>> End job, result: " + count);
                logger.info(">>>> Unlock our lock with key: " + key);
                key.ifPresent(s -> etcdLock.releaseLock(s));
            }
        } catch (InterruptedException e) {
            logger.error(e.getLocalizedMessage());
        } catch (ExecutionException e) {
            logger.error(e.getLocalizedMessage());
        }

    }
}
