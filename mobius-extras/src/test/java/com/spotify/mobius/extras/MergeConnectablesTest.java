/*
 * -\-\-
 * Mobius
 * --
 * Copyright (c) 2017-2020 Spotify AB
 * --
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -/-/-
 */
package com.spotify.mobius.extras;

import static com.spotify.mobius.extras.TestConnectable.State.CONNECTED;
import static com.spotify.mobius.extras.TestConnectable.State.DISPOSED;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.Assert.assertEquals;

import com.spotify.mobius.Connectable;
import com.spotify.mobius.Connection;
import com.spotify.mobius.extras.domain.B;
import com.spotify.mobius.extras.domain.C;
import com.spotify.mobius.test.RecordingConsumer;
import org.junit.Before;
import org.junit.Test;

public class MergeConnectablesTest {
  TestConnectable c1;
  TestConnectable c2;
  RecordingConsumer<C> consumer;
  private Connectable<B, C> merged;
  private Connection<B> connection;

  @Before
  public void setUp() throws Exception {
    c1 = TestConnectable.createWithReversingTransformation();
    c2 = TestConnectable.create(String::toLowerCase);
    consumer = new RecordingConsumer<>();
    merged = Connectables.merge(c1, c2);
  }

  @Test
  public void propagatesConnectionImmediately() {
    connect();
    assertEquals(CONNECTED, c1.state);
    assertEquals(CONNECTED, c2.state);
  }

  @Test
  public void propagatesMultipleConnections() {
    merged.connect(consumer);
    merged.connect(consumer);

    assertEquals(2, c1.connectionsCount);
    assertEquals(2, c2.connectionsCount);
  }

  @Test
  public void propagatesConnectionDisposal() {
    connect();
    connection.dispose();
    assertEquals(DISPOSED, c1.state);
    assertEquals(DISPOSED, c2.state);
  }

  @Test
  public void appliesBothChildrenOnInputAndForwardsOutput() {
    connect();
    connection.accept(B.create("Hello"));
    consumer.assertValues(C.create("olleH"), C.create("hello"));
  }

  @Test
  public void acceptingValuesAfterDisposalThrowsException() {
    connect();
    connection.dispose();
    assertThatThrownBy(
            () -> connection.accept(B.create("I know I shouldn't do this, but I am anyway!")))
        .isInstanceOf(IllegalStateException.class);
  }

  private void connect() {
    connection = merged.connect(consumer);
  }
}
