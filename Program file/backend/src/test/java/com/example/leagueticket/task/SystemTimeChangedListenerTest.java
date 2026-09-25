package com.example.leagueticket.task;

import com.example.leagueticket.service.LifecycleCompensationService;
import com.example.leagueticket.service.SystemTimeChangedEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SystemTimeChangedListenerTest {
    @Test
    void invokesSynchronousCompensationAndDoesNotTurnCommittedTimeChangeIntoFailure() {
        LifecycleCompensationService compensation = mock(LifecycleCompensationService.class);
        SystemTimeChangedListener listener = new SystemTimeChangedListener(compensation);
        SystemTimeChangedEvent event = new SystemTimeChangedEvent(LocalDateTime.of(2048, 1, 1, 0, 0), "SET");

        listener.afterSystemTimeChanged(event);
        verify(compensation).catchUpAfterSystemTimeChange();

        doThrow(new IllegalStateException("one bad lifecycle entity")).when(compensation).catchUpAfterSystemTimeChange();
        assertThatCode(() -> listener.afterSystemTimeChanged(event)).doesNotThrowAnyException();
    }
}
