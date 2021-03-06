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
package com.spotify.mobius.rx;

import com.spotify.mobius.EventSource;
import com.spotify.mobius.disposables.Disposable;
import com.spotify.mobius.test.RecordingConsumer;
import org.junit.Rule;
import org.junit.Test;
import rx.Observable;
import rx.subjects.PublishSubject;

public class RxEventSourcesTest {

  @Rule public final RxErrorsRule rxErrorsRule = new RxErrorsRule();

  @Test
  public void eventsAreForwardedInOrder() throws Exception {
    EventSource<Integer> source = RxEventSources.fromObservables(Observable.just(1, 2, 3));
    RecordingConsumer<Integer> consumer = new RecordingConsumer<>();

    source.subscribe(consumer);

    consumer.waitForChange(50);
    consumer.assertValues(1, 2, 3);
  }

  @Test
  public void disposePreventsFurtherEvents() throws Exception {
    PublishSubject<Integer> subject = PublishSubject.create();
    EventSource<Integer> source = RxEventSources.fromObservables(subject);
    RecordingConsumer<Integer> consumer = new RecordingConsumer<>();

    Disposable d = source.subscribe(consumer);

    subject.onNext(1);
    subject.onNext(2);
    d.dispose();
    subject.onNext(3);

    consumer.waitForChange(50);
    consumer.assertValues(1, 2);
  }

  @Test
  public void errorsAreForwardedToErrorHandler() throws Exception {
    PublishSubject<Integer> subject = PublishSubject.create();
    final EventSource<Integer> source = RxEventSources.fromObservables(subject);
    RecordingConsumer<Integer> consumer = new RecordingConsumer<>();

    source.subscribe(consumer);
    subject.onError(new RuntimeException("crash!"));

    rxErrorsRule.assertSingleErrorWithMessage("crash!");
  }
}
