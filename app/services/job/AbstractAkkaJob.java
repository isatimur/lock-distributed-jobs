package services.job;

/**
 * ${CLASS_NAME}. Created at 4/2/2018 10:08 AM by
 * @author Timur Isachenko
 */

import akka.actor.ActorSystem;
import com.typesafe.config.Config;
import play.Environment;
import play.Logger;
import play.libs.Time;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.Duration;

import java.text.ParseException;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

public abstract class AbstractAkkaJob {

    protected final Logger.ALogger logger = Logger.of(getClass());

    protected final ExecutionContext executionContext;

    private final ActorSystem actorSystem;

    private final Config config;

    private boolean stopOnException;

    @Inject
    private Environment environment;

    public AbstractAkkaJob(ActorSystem actorSystem, ExecutionContext executionContext, final Config config) {
        this.actorSystem = actorSystem;
        this.executionContext = executionContext;
        this.config = config;
    }

    protected abstract void process() throws ExecutionException, InterruptedException;

    protected void start(String cronExpressionProperty) throws ParseException {

//        if (environment.isTest()) return;
        logger.debug(">> init");

        Time.CronExpression cron = new Time.CronExpression(config.getString(cronExpressionProperty));
        Date now = new Date();
        Date nextStartDateTime = cron.getNextValidTimeAfter(now);
        long interval = cron.getNextInterval(now);
        long delay = nextStartDateTime.getTime() - now.getTime();

        this.actorSystem.scheduler().schedule(Duration.create(delay, TimeUnit.MILLISECONDS), // initialDelay
                Duration.create(interval, TimeUnit.MILLISECONDS), // interval
                () -> {
                    try {
                        process();
                    } catch (Exception e) {
                        if (!stopOnException) {                             //logging, but continue execute job
                            logger.error(e.getMessage(), e);
                        } else {
                            if (e instanceof RuntimeException) {
                                throw (RuntimeException) e;
                            } else {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }, this.executionContext);

        logger.info("created scheduler: nextStart = {}, nextNextStart = {}, delayMs = {}, intervalMs = {}", new Date(now.getTime() + delay), new Date(now.getTime() + delay + interval), delay, interval);
        logger.debug("<< init");
    }

    /**
     * enable stop when catch exception in process() method
     */
    protected void enableStopOnException() {
        stopOnException = true;
    }
}