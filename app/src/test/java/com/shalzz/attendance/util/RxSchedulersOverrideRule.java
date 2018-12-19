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

package com.shalzz.attendance.util;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.concurrent.Callable;

import io.reactivex.Scheduler;
import io.reactivex.android.plugins.RxAndroidPlugins;
import io.reactivex.functions.Function;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;

/**
 * This rule registers Handlers for RxJava and RxAndroid to ensure that subscriptions
 * always subscribeOn and observeOn Schedulers.trampoline().
 * Warning, this rule will reset RxAndroidPlugins and RxJavaPlugins before and after each test so
 * if the application code uses RxJava plugins this may affect the behaviour of the testing method.
 */
public class RxSchedulersOverrideRule implements TestRule {

    private final Function<Callable<Scheduler>, Scheduler> mRxAndroidSchedulersHook =
            schedulerCallable -> getScheduler();

    private final Function<Scheduler, Scheduler> mRxJavaImmediateScheduler =
            scheduler -> getScheduler();

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                RxAndroidPlugins.reset();
                RxAndroidPlugins.setInitMainThreadSchedulerHandler(mRxAndroidSchedulersHook);

                RxJavaPlugins.reset();
                RxJavaPlugins.setIoSchedulerHandler(mRxJavaImmediateScheduler);
                RxJavaPlugins.setSingleSchedulerHandler(mRxJavaImmediateScheduler);
                RxJavaPlugins.setNewThreadSchedulerHandler(mRxJavaImmediateScheduler);

                base.evaluate();

                RxAndroidPlugins.reset();
                RxJavaPlugins.reset();
            }
        };
    }

    public Scheduler getScheduler() {
        return Schedulers.trampoline();
    }

}