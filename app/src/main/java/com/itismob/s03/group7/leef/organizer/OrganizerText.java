package com.itismob.s03.group7.leef.organizer;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.data.AttendeeRepository;

/** Words for the things that can go wrong on the organizer screens. */
final class OrganizerText {

    private OrganizerText() {
    }

    /** Why a list couldn't be loaded. */
    @StringRes
    static int loadError(@NonNull AttendeeRepository.Failure failure) {
        switch (failure) {
            case NETWORK:
                return R.string.home_error_network;
            case PERMISSION:
                return R.string.organizer_error_permission;
            default:
                return R.string.home_error_generic;
        }
    }

    /** Why a check-in didn't go through. */
    @StringRes
    static int checkInError(@NonNull AttendeeRepository.Failure failure) {
        switch (failure) {
            case NETWORK:
                return R.string.organizer_check_in_offline;
            case PERMISSION:
                return R.string.organizer_error_permission;
            default:
                return R.string.organizer_check_in_failed;
        }
    }
}
