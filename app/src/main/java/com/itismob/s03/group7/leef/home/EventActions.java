package com.itismob.s03.group7.leef.home;

import androidx.annotation.NonNull;

import com.itismob.s03.group7.leef.model.Event;

/** What a tap on an event card can do. */
interface EventActions {

    /** The card itself was tapped (opens Event Details). */
    void onEventClick(@NonNull Event event);

    /** The heart was tapped (save or un-save). */
    void onSaveClick(@NonNull Event event);
}
