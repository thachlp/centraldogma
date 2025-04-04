/*
 * Copyright 2020 LINE Corporation
 *
 * LINE Corporation licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.linecorp.centraldogma.server.internal.mirror;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Streams;

import com.linecorp.armeria.common.util.UnmodifiableFuture;
import com.linecorp.centraldogma.server.command.CommandExecutor;
import com.linecorp.centraldogma.server.credential.Credential;
import com.linecorp.centraldogma.server.mirror.Mirror;
import com.linecorp.centraldogma.server.mirror.MirrorAccessController;
import com.linecorp.centraldogma.server.mirror.MirrorDirection;
import com.linecorp.centraldogma.server.mirror.MirrorResult;
import com.linecorp.centraldogma.server.mirror.MirrorStatus;
import com.linecorp.centraldogma.server.storage.project.Project;
import com.linecorp.centraldogma.server.storage.project.ProjectManager;
import com.linecorp.centraldogma.server.storage.repository.MetaRepository;
import com.linecorp.centraldogma.server.storage.repository.Repository;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class MirrorSchedulingServiceTest {

    @TempDir
    static File temporaryFolder;

    private static final Cron EVERY_SECOND = new CronParser(
            CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ)).parse("* * * * * ?");

    @Test
    void mirroringTaskShouldNeverBeRejected() {
        final AtomicInteger taskCounter = new AtomicInteger();
        final ProjectManager pm = mock(ProjectManager.class);
        final Project p = mock(Project.class);
        final MetaRepository mr = mock(MetaRepository.class);
        final Repository r = mock(Repository.class);
        when(pm.list()).thenReturn(ImmutableMap.of("foo", p));
        when(p.name()).thenReturn("foo");
        when(p.metaRepo()).thenReturn(mr);
        when(r.parent()).thenReturn(p);
        when(r.name()).thenReturn("bar");

        final Mirror mirror = new AbstractMirror("my-mirror-1", true, EVERY_SECOND,
                                                 MirrorDirection.REMOTE_TO_LOCAL,
                                                 Credential.NONE, r, "/",
                                                 URI.create("unused://uri"), "/", "", null, null) {
            @Override
            protected MirrorResult mirrorLocalToRemote(File workDir, int maxNumFiles, long maxNumBytes,
                                                       Instant triggeredTime) {
                return newMirrorResult(MirrorStatus.UP_TO_DATE, null, Instant.now());
            }

            @Override
            protected MirrorResult mirrorRemoteToLocal(File workDir, CommandExecutor executor,
                                                       int maxNumFiles, long maxNumBytes, Instant triggeredTime)
                    throws Exception {
                // Sleep longer than mirroring interval so that the workers fall behind.
                taskCounter.incrementAndGet();
                Thread.sleep(2000);
                return newMirrorResult(MirrorStatus.SUCCESS, null, Instant.now());
            }
        };

        when(mr.mirrors()).thenReturn(CompletableFuture.completedFuture(ImmutableList.of(mirror)));

        final MirrorSchedulingService service = new MirrorSchedulingService(
                temporaryFolder, pm, new SimpleMeterRegistry(), 1, 1, 1, null, false,
                AlwaysAllowedMirrorAccessController.INSTANCE);
        final CommandExecutor executor = mock(CommandExecutor.class);
        service.start(executor);

        try {
            // The mirroring task should run more than once.
            await().until(() -> taskCounter.get() > 1);
        } finally {
            service.stop();
        }
    }

    private enum AlwaysAllowedMirrorAccessController implements MirrorAccessController {

        INSTANCE;

        @Override
        public CompletableFuture<Boolean> allow(String targetPattern, String reason,
                                                int order) {
            return UnmodifiableFuture.completedFuture(true);
        }

        @Override
        public CompletableFuture<Boolean> disallow(String targetPattern, String reason,
                                                   int order) {
            return UnmodifiableFuture.completedFuture(true);
        }

        @Override
        public CompletableFuture<Boolean> isAllowed(Mirror mirror) {
            return UnmodifiableFuture.completedFuture(true);
        }

        @Override
        public CompletableFuture<Boolean> isAllowed(String repoUri) {
            return UnmodifiableFuture.completedFuture(true);
        }

        @Override
        public CompletableFuture<Map<String, Boolean>> isAllowed(Iterable<String> repoUris) {
            return UnmodifiableFuture.completedFuture(
                    Streams.stream(repoUris)
                           .collect(toImmutableMap(uri -> uri, uri -> true)));
        }
    }
}
