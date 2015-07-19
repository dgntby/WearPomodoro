package com.tasomaniac.android.pomodoro.wear;

import com.tasomaniac.android.pomodoro.shared.PomodoroModule;
import com.tasomaniac.android.pomodoro.wear.service.PomodoroWearableListenerService;
import com.tasomaniac.android.pomodoro.wear.service.PomodoroNotificationService;
import com.tasomaniac.android.pomodoro.wear.ui.PomodoroNotificationActivity;

import javax.inject.Singleton;

import dagger.Component;

/**
 * Dagger 2 Component containing parts of the application.
 *
 * Created by Said Tahsin Dane on 17/03/15.
 */
@Singleton
@Component(modules = { AppModule.class, PomodoroModule.class })
public interface PomodoroComponent {

    void inject(App app);
    void inject(PomodoroNotificationService receiver);
    void inject(PomodoroWearableListenerService service);
    void inject(PomodoroNotificationActivity activity);

    /**
     * An initializer that creates the graph from an application.
     */
    final class Initializer {
        static PomodoroComponent init(App app) {
            return DaggerPomodoroComponent.builder()
                    .appModule(new AppModule(app))
                    .build();
        }
        private Initializer() {} // No instances.
    }
}
