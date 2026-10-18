package com.itismob.s03.group7.leef.postevent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.data.EventRepository;
import com.itismob.s03.group7.leef.data.FirebaseEventRepository;
import com.itismob.s03.group7.leef.data.FirebaseProfileRepository;
import com.itismob.s03.group7.leef.data.ProfileRepository;
import com.itismob.s03.group7.leef.home.FeedChange;
import com.itismob.s03.group7.leef.model.EventAudience;
import com.itismob.s03.group7.leef.model.EventCategory;
import com.itismob.s03.group7.leef.model.EventDraft;
import com.itismob.s03.group7.leef.model.UserProfile;
import com.itismob.s03.group7.leef.model.UserRole;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Screen 11 - Create / Post Event. Holds what the organizer picked (category, dates, audience, tags,
 * pricing mode, photo link), checks the form, and saves the event. The typed text is handed over by the
 * screen when the person taps Post Event, so this class never depends on views.
 */
public class CreateEventViewModel extends ViewModel {

    public static final int MAX_TAGS = 5;
    public static final int MAX_TAG_LENGTH = 24;
    public static final int MAX_PRICE = 100000;

    /** Result of trying to add a tag. */
    public enum TagResult {
        ADDED, EMPTY, DUPLICATE, FULL
    }

    private final ProfileRepository profiles;
    private final EventRepository events;

    private final MutableLiveData<CreateEventState> state = new MutableLiveData<>();
    private final MutableLiveData<EventFormErrors> errors = new MutableLiveData<>(new EventFormErrors());
    private final MutableLiveData<Integer> message = new MutableLiveData<>();
    private final MutableLiveData<String> posted = new MutableLiveData<>();

    @Nullable
    private UserProfile profile;
    private CreateEventState.Status status = CreateEventState.Status.LOADING;
    private int errorRes;
    private boolean posting;

    @Nullable
    private EventCategory category;
    private long startsAt;
    private long endsAt;
    private final Set<EventAudience> audiences = new LinkedHashSet<>();
    private final List<String> tags = new ArrayList<>();
    private boolean paid;
    private boolean earlyBirdOn;
    private long earlyBirdUntil;
    private boolean groupOn;
    @Nullable
    private String coverUrl;

    public CreateEventViewModel() {
        this(new FirebaseProfileRepository(), new FirebaseEventRepository());
    }

    CreateEventViewModel(@NonNull ProfileRepository profiles, @NonNull EventRepository events) {
        this.profiles = profiles;
        this.events = events;
        publish();
        loadProfile();
    }

    @NonNull
    public LiveData<CreateEventState> getState() {
        return state;
    }

    /** The problems found by the last Post Event tap. */
    @NonNull
    public LiveData<EventFormErrors> getErrors() {
        return errors;
    }

    /** A toast message (string resource id). Call {@link #messageHandled()} after showing it. */
    @NonNull
    public LiveData<Integer> getMessage() {
        return message;
    }

    public void messageHandled() {
        message.setValue(null);
    }

    /** The id of the event that was just posted; handle once, then call {@link #postedHandled()}. */
    @NonNull
    public LiveData<String> getPosted() {
        return posted;
    }

    public void postedHandled() {
        posted.setValue(null);
    }

    public void retry() {
        if (status == CreateEventState.Status.ERROR) {
            status = CreateEventState.Status.LOADING;
            publish();
            loadProfile();
        }
    }

    // ---- what the person picked -------------------------------------------------------------

    @Nullable
    public EventCategory getCategory() {
        return category;
    }

    public void setCategory(@Nullable EventCategory value) {
        category = value;
        clearError(e -> e.category = 0);
    }

    /** Start of the event in milliseconds, 0 until picked. */
    public long getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(long millis) {
        startsAt = millis;
        clearError(e -> {
            e.start = 0;
            e.end = 0;
            e.earlyUntil = 0;
        });
    }

    /** End of the event in milliseconds, 0 when there is no separate end time. */
    public long getEndsAt() {
        return endsAt;
    }

    public void setEndsAt(long millis) {
        endsAt = millis;
        clearError(e -> e.end = 0);
    }

    @NonNull
    public Set<EventAudience> getAudiences() {
        return audiences;
    }

    public void setAudience(@NonNull EventAudience audience, boolean selected) {
        if (selected) {
            audiences.add(audience);
        } else {
            audiences.remove(audience);
        }
    }

    @NonNull
    public List<String> getTags() {
        return tags;
    }

    /** Adds a tag the person typed ("#robotics", " AI  tools "); returns what happened to it. */
    @NonNull
    public TagResult addTag(@NonNull String raw) {
        String tag = normalizeTag(raw);
        if (tag.isEmpty()) {
            return TagResult.EMPTY;
        }
        for (String existing : tags) {
            if (existing.equalsIgnoreCase(tag)) {
                return TagResult.DUPLICATE;
            }
        }
        if (tags.size() >= MAX_TAGS) {
            return TagResult.FULL;
        }
        tags.add(tag);
        return TagResult.ADDED;
    }

    public void removeTag(@NonNull String tag) {
        tags.remove(tag);
    }

    @NonNull
    static String normalizeTag(@NonNull String raw) {
        String tag = raw.trim().replaceAll("\\s+", " ");
        while (tag.startsWith("#")) {
            tag = tag.substring(1).trim();
        }
        if (tag.length() > MAX_TAG_LENGTH) {
            tag = tag.substring(0, MAX_TAG_LENGTH).trim();
        }
        return tag;
    }

    public boolean isPaid() {
        return paid;
    }

    public void setPaid(boolean value) {
        paid = value;
        clearError(e -> {
            e.price = 0;
            e.earlyPrice = 0;
            e.earlyUntil = 0;
            e.groupPrice = 0;
        });
    }

    public boolean isEarlyBirdOn() {
        return earlyBirdOn;
    }

    public void setEarlyBirdOn(boolean value) {
        earlyBirdOn = value;
        clearError(e -> {
            e.earlyPrice = 0;
            e.earlyUntil = 0;
        });
    }

    /** The last moment of the early-bird day in milliseconds, 0 until picked. */
    public long getEarlyBirdUntil() {
        return earlyBirdUntil;
    }

    /** @param dayMillis any moment on the chosen day; the offer lasts until the end of that day */
    public void setEarlyBirdUntilDay(long dayMillis) {
        Calendar end = Calendar.getInstance();
        end.setTimeInMillis(dayMillis);
        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 59);
        end.set(Calendar.SECOND, 59);
        end.set(Calendar.MILLISECOND, 0);
        earlyBirdUntil = end.getTimeInMillis();
        clearError(e -> e.earlyUntil = 0);
    }

    public boolean isGroupOn() {
        return groupOn;
    }

    public void setGroupOn(boolean value) {
        groupOn = value;
        clearError(e -> e.groupPrice = 0);
    }

    @Nullable
    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(@Nullable String url) {
        coverUrl = url;
    }

    // ---- posting ----------------------------------------------------------------------------

    /** The text the person typed, handed over when they tap Post Event. */
    public static final class FormText {
        public final String title;
        public final String description;
        public final String venue;
        public final String city;
        public final String price;
        public final String earlyPrice;
        public final String groupPrice;

        public FormText(@NonNull String title, @NonNull String description, @NonNull String venue,
                        @NonNull String city, @NonNull String price, @NonNull String earlyPrice,
                        @NonNull String groupPrice) {
            this.title = title.trim();
            this.description = description.trim();
            this.venue = venue.trim();
            this.city = city.trim();
            this.price = price;
            this.earlyPrice = earlyPrice;
            this.groupPrice = groupPrice;
        }
    }

    /** Checks the form and, when it is fine, saves the event. */
    public void post(@NonNull FormText text) {
        if (posting || status != CreateEventState.Status.FORM || profile == null) {
            return;
        }
        long now = System.currentTimeMillis();
        EventFormErrors found = new EventFormErrors();

        if (text.title.isEmpty()) {
            found.title = R.string.create_error_title;
        }
        if (category == null) {
            found.category = R.string.create_error_category;
        }
        if (startsAt <= 0) {
            found.start = R.string.create_error_start;
        } else if (startsAt <= now) {
            found.start = R.string.create_error_start_past;
        }
        if (endsAt > 0 && startsAt > 0 && endsAt <= startsAt) {
            found.end = R.string.create_error_end;
        }
        if (text.venue.isEmpty()) {
            found.venue = R.string.create_error_venue;
        }

        double price = 0;
        double earlyPrice = -1;
        long earlyUntil = 0;
        double groupPrice = -1;
        if (paid) {
            price = parsePrice(text.price);
            if (Double.isNaN(price) || price <= 0 || price > MAX_PRICE) {
                found.price = R.string.create_error_price;
                price = 0;
            }
            if (earlyBirdOn) {
                earlyPrice = parsePrice(text.earlyPrice);
                if (Double.isNaN(earlyPrice) || earlyPrice <= 0 || earlyPrice > MAX_PRICE) {
                    found.earlyPrice = R.string.create_error_price;
                } else if (found.price == 0 && earlyPrice >= price) {
                    found.earlyPrice = R.string.create_error_early_price;
                }
                if (earlyBirdUntil <= 0) {
                    found.earlyUntil = R.string.create_error_early_until;
                } else if (earlyBirdUntil <= now) {
                    found.earlyUntil = R.string.create_error_early_until_past;
                } else if (startsAt > 0 && earlyBirdUntil > startsAt) {
                    found.earlyUntil = R.string.create_error_early_until_late;
                }
                earlyUntil = earlyBirdUntil;
            }
            if (groupOn) {
                groupPrice = parsePrice(text.groupPrice);
                if (Double.isNaN(groupPrice) || groupPrice <= 0 || groupPrice > MAX_PRICE) {
                    found.groupPrice = R.string.create_error_price;
                } else if (found.price == 0 && groupPrice >= price) {
                    found.groupPrice = R.string.create_error_group_price;
                }
            }
        }
        errors.setValue(found);
        if (found.hasAny()) {
            return;
        }

        EventDraft draft = new EventDraft(text.title, text.description, category, startsAt,
                endsAt > startsAt ? endsAt : 0, text.venue, text.city,
                new ArrayList<>(audiences), new ArrayList<>(tags), price,
                earlyBirdOn && paid ? earlyPrice : -1, earlyBirdOn && paid ? earlyUntil : 0,
                groupOn && paid ? groupPrice : -1, coverUrl, profile.getSchoolOrganization());
        posting = true;
        publish();
        events.postEvent(draft, new EventRepository.PostedCallback() {
            @Override
            public void onPosted(@NonNull String eventId) {
                posting = false;
                FeedChange.bump();
                publish();
                posted.setValue(eventId);
            }

            @Override
            public void onFailure(@NonNull EventRepository.Failure failure) {
                posting = false;
                publish();
                message.setValue(failureMessage(failure));
            }
        });
    }

    @StringRes
    private static int failureMessage(@NonNull EventRepository.Failure failure) {
        switch (failure) {
            case NETWORK:
                return R.string.create_post_offline;
            case PERMISSION:
                return R.string.create_post_denied;
            default:
                return R.string.create_post_failed;
        }
    }

    /** Pesos typed by the person ("1,500", "₱350.50") as a number, or NaN when it isn't one. */
    static double parsePrice(@NonNull String raw) {
        String cleaned = raw.replace("₱", "").replace(",", "").replace(" ", "").trim();
        if (cleaned.isEmpty() || cleaned.length() > 9) {
            return Double.NaN;
        }
        try {
            double value = Double.parseDouble(cleaned);
            return Double.isInfinite(value) ? Double.NaN : Math.round(value * 100) / 100.0;
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    // ---- profile ----------------------------------------------------------------------------

    private void loadProfile() {
        profiles.loadProfile(new ProfileRepository.LoadCallback() {
            @Override
            public void onLoaded(@NonNull UserProfile loaded) {
                profile = loaded;
                status = loaded.getRole() == UserRole.ORGANIZER
                        ? CreateEventState.Status.FORM : CreateEventState.Status.FORBIDDEN;
                publish();
            }

            @Override
            public void onMissing() {
                status = CreateEventState.Status.ERROR;
                errorRes = R.string.create_error_profile;
                publish();
            }

            @Override
            public void onFailure(@NonNull ProfileRepository.Failure failure) {
                status = CreateEventState.Status.ERROR;
                errorRes = failure == ProfileRepository.Failure.NETWORK
                        ? R.string.home_error_network : R.string.create_error_profile;
                publish();
            }
        });
    }

    private void publish() {
        state.setValue(new CreateEventState(status, posting,
                profile == null ? null : profile.getSchoolOrganization(), errorRes));
    }

    private interface ErrorEdit {
        void apply(@NonNull EventFormErrors errors);
    }

    /** Clears a field's error as soon as the person changes it. */
    private void clearError(@NonNull ErrorEdit edit) {
        EventFormErrors current = errors.getValue();
        if (current != null && current.hasAny()) {
            edit.apply(current);
            errors.setValue(current);
        }
    }
}
