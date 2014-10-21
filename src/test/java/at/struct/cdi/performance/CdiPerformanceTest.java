/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package at.struct.cdi.performance;

import java.util.concurrent.TimeUnit;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

import org.apache.deltaspike.cdise.api.CdiContainer;
import org.apache.deltaspike.cdise.api.CdiContainerLoader;
import org.apache.deltaspike.cdise.api.ContextControl;
import org.testng.annotations.Test;

import at.struct.cdi.performance.beans.InjectedBean;
import at.struct.cdi.performance.beans.SimpleBeanWithoutInterceptor;

/**
 * A few micro benchmarks for various CDI stuff
 * @author <a href="mailto:struberg@yahoo.de">Mark Struberg</a>
 */
public class CdiPerformanceTest
{
    private static int NUM_THREADS = 100;
    private static int NUM_ITERATION=100000000;


    private void warmUp(BeanManager manager) {
        System.out.println("Warming up");
        SimpleBeanWithoutInterceptor underTest = getInstance(manager, SimpleBeanWithoutInterceptor.class);
        for (int i = 0; i < 10000; i++) {
            underTest.theMeaningOfLife();
        }
        System.out.println("Done");
    }

    @Test
    public void testNormalScopePerformance() throws InterruptedException
    {
        CdiContainer cdiContainer = CdiContainerLoader.getCdiContainer();
        cdiContainer.boot();
        final ContextControl contextControl = cdiContainer.getContextControl();

        warmUp(cdiContainer.getBeanManager());

        final SimpleBeanWithoutInterceptor underTest = getInstance(cdiContainer.getBeanManager(), InjectedBean.class).getSimpleBean();

        executeInParallel(new Runnable()
        {
            @Override
            public void run()
            {
                // disable for now as it looks that this does not work properly ATM
                //contextControl.startContext(RequestScoped.class);

                for (int i = 0; i < NUM_ITERATION; i++)
                {
                    // this line does the actual bean invocation.
                    underTest.theMeaningOfLife();
                }

                //contextControl.stopContext(RequestScoped.class);
            }
        });

        System.out.println("count = " + underTest.getCount());

        cdiContainer.shutdown();
    }

    private void executeInParallel(Runnable runnable) throws InterruptedException
    {
        Thread[] threads = new Thread[NUM_THREADS];

        for (int i = 0; i < NUM_THREADS; i++)
        {
            threads[i] = new Thread(runnable);
        }

        long start = System.nanoTime();

        for (int i = 0; i < NUM_THREADS; i++)
        {
            threads[i].start();
        }

        for (int i = 0; i < NUM_THREADS; i++)
        {
            threads[i].join();
        }
        long end = System.nanoTime();

        System.out.println("\n\n\tALL THE STUFF TOOK: " + TimeUnit.NANOSECONDS.toMillis(end - start) + " ms\n\n");
    }


    private <T> T getInstance(BeanManager bm, Class<T> clazz) {
        Bean<?> bean = bm.resolve(bm.getBeans(clazz));
        return (T) bm.getReference(bean, clazz, bm.createCreationalContext(bean));
    }
}
