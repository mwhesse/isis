package demoapp.dom.types.javautil.javautildate.jdo;

import java.util.Date;

import javax.inject.Inject;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import org.apache.isis.applib.services.repository.RepositoryService;
import org.apache.isis.core.runtime.events.app.AppLifecycleEvent;
import org.apache.isis.testing.fixtures.applib.fixturescripts.FixtureScript;
import org.apache.isis.testing.fixtures.applib.fixturescripts.FixtureScripts;

import demoapp.dom._infra.seed.SeedServiceAbstract;
import demoapp.dom.types.Samples;

@Service
public class JavaUtilDateJdoSeedService extends SeedServiceAbstract {

    public JavaUtilDateJdoSeedService() {
        super(TemporalJavaUtilDateJdoEntityFixture::new);
    }

    static class TemporalJavaUtilDateJdoEntityFixture extends FixtureScript {

        @Override
        protected void execute(ExecutionContext executionContext) {
            samples.stream()
                    .map(JavaUtilDateJdo::new)
                    .forEach(repositoryService::persist);
        }

        @Inject
        RepositoryService repositoryService;

        @Inject
        Samples<Date> samples;
    }
}