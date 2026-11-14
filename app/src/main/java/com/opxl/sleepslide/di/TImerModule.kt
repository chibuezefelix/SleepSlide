package com.opxl.sleepslide.di

import com.opxl.sleepslide.data.timer.AlarmTimerBackstop
import com.opxl.sleepslide.data.timer.TimerBackstop
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class TimerModule {

    @Binds
    abstract fun bindTimerBackstop(impl: AlarmTimerBackstop): TimerBackstop
}
