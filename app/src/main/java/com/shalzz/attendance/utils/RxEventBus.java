/*
 * Copyright (c) 2013-2018 Shaleen Jain <shaleen.jain95@gmail.com>
 *
 * This file is part of College Academics.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.shalzz.attendance.utils;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.subjects.PublishSubject;

/**
 * A simple event bus built with RxJava
 */
@Singleton
public class RxEventBus {

    private final BackpressureStrategy mBackpressureStrategy = BackpressureStrategy.BUFFER;
    private final PublishSubject<Object> mBusSubject;

    @Inject
    public RxEventBus() {
        mBusSubject = PublishSubject.create();
    }

    /**
     * Posts an object (usually an Event) to the bus
     */
    public void post(Object event) {
        mBusSubject.onNext(event);
    }

    /**
     * Observable that will emmit everything posted to the event bus.
     */
    public Flowable<Object> observable() {
        return mBusSubject.toFlowable(mBackpressureStrategy);
    }

    /**
     * Observable that only emits events of a specific class.
     * Use this if you only want to subscribe to one type of events.
     */
    public <T> Flowable<T> filteredObservable(final Class<T> eventClass) {
        return mBusSubject.ofType(eventClass).toFlowable(mBackpressureStrategy);
    }

}