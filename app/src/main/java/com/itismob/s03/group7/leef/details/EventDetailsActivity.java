package com.itismob.s03.group7.leef.details;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.checkin.CheckInActivity;
import com.itismob.s03.group7.leef.databinding.ActivityEventDetailsBinding;
import com.itismob.s03.group7.leef.feedback.FeedbackActivity;
import com.itismob.s03.group7.leef.feedback.FeedbackListActivity;
import com.itismob.s03.group7.leef.home.EventCardBinder;
import com.itismob.s03.group7.leef.home.EventFormat;
import com.itismob.s03.group7.leef.home.EventItem;
import com.itismob.s03.group7.leef.home.HomeViewModel;
import com.itismob.s03.group7.leef.model.Event;
import com.itismob.s03.group7.leef.registration.RegistrationActivity;
import com.itismob.s03.group7.leef.ui.LeefEdgeToEdge;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Screen 6 - Event Details: the complete listing (photo, category, title, summary, date and time,
 * venue with a map link, organizer, tags and pricing), the heart, share, and Register Now, which opens
 * Registration &amp; Payment (Screen 7).
 */
public class EventDetailsActivity extends AppCompatActivity {

    private static final String EXTRA_EVENT_ID = "event_id";

    private ActivityEventDetailsBinding binding;
    private EventDetailsViewModel viewModel;
    private ActivityResultLauncher<Intent> registrationLauncher;
    private String eventId;

    @NonNull
    public static Intent createIntent(@NonNull Context context, @NonNull String eventId) {
        return new Intent(context, EventDetailsActivity.class).putExtra(EXTRA_EVENT_ID, eventId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LeefEdgeToEdge.enable(this);

        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        if (eventId == null || eventId.isEmpty()) {
            finish();
            return;
        }

        binding = ActivityEventDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyWindowInsets();

        viewModel = new ViewModelProvider(this, new EventDetailsViewModel.Factory(eventId))
                .get(EventDetailsViewModel.class);

        registrationLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                viewModel.markRegistered();
            }
        });

        binding.backButton.setOnClickListener(v -> finish());
        binding.retryButton.setOnClickListener(v -> viewModel.retry());
        binding.notFoundBackButton.setOnClickListener(v -> finish());
        binding.heart.setOnClickListener(v -> viewModel.toggleSaved());

        viewModel.getState().observe(this, this::render);
        viewModel.getMessage().observe(this, messageRes -> {
            if (messageRes != null) {
                Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show();
                viewModel.messageHandled();
            }
        });
    }

    private void render(@NonNull EventDetailsState state) {
        EventDetailsState.Status status = state.getStatus();
        boolean content = status == EventDetailsState.Status.CONTENT && state.getEvent() != null;

        binding.loading.setVisibility(status == EventDetailsState.Status.LOADING ? View.VISIBLE : View.GONE);
        binding.errorGroup.setVisibility(status == EventDetailsState.Status.ERROR ? View.VISIBLE : View.GONE);
        binding.notFoundGroup.setVisibility(status == EventDetailsState.Status.NOT_FOUND ? View.VISIBLE : View.GONE);
        binding.scroll.setVisibility(content ? View.VISIBLE : View.GONE);
        binding.bottomBar.setVisibility(content ? View.VISIBLE : View.GONE);
        binding.heart.setVisibility(content ? View.VISIBLE : View.INVISIBLE);
        binding.share.setVisibility(content ? View.VISIBLE : View.INVISIBLE);

        if (status == EventDetailsState.Status.ERROR) {
            binding.errorText.setText(HomeViewModel.errorMessageFor(state.getFailure()));
        }
        if (content) {
            bindEvent(state.getEvent(), state.isSaved(), state.isRegistered());
        }
    }

    private void bindEvent(@NonNull Event event, boolean saved, boolean registered) {
        long now = System.currentTimeMillis();

        EventCardBinder.bindHeart(binding.heart, new EventItem(event, saved));
        binding.share.setOnClickListener(v -> share(event));

        EventCardBinder.bindCover(binding.coverPhoto, event.getCoverUrl());
        EventCardBinder.bindBadge(binding.badge, event);
        binding.title.setText(event.getTitle());
        setTextOrHide(binding.description, event.getDescription());

        // Date and time
        binding.dateText.setText(EventFormat.detailDateText(event));
        setTextOrHide(binding.timeText, EventFormat.timeText(this, event));

        // Venue and map
        String venue = event.getVenue();
        String area = !event.getAddress().isEmpty() ? event.getAddress() : event.getCity();
        // With no venue name, the address (or city) takes the first line.
        String firstLine = !venue.isEmpty() ? venue : area;
        String secondLine = !venue.isEmpty() ? area : "";
        boolean hasPlace = !firstLine.isEmpty();
        binding.venueRow.setVisibility(hasPlace ? View.VISIBLE : View.GONE);
        binding.venueText.setText(firstLine);
        setTextOrHide(binding.areaText, secondLine);
        binding.viewMapButton.setOnClickListener(v -> openMap(event));

        // Organizer
        boolean hasOrganizer = !event.getOrganizerName().isEmpty();
        binding.organizerRow.setVisibility(hasOrganizer ? View.VISIBLE : View.GONE);
        binding.organizerName.setText(event.getOrganizerName());
        bindOrganizerLogo(event);

        // Tags
        binding.tags.removeAllViews();
        binding.tags.setVisibility(event.getTags().isEmpty() ? View.GONE : View.VISIBLE);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (String tag : event.getTags()) {
            TextView pill = (TextView) inflater.inflate(R.layout.item_event_tag, binding.tags, false);
            pill.setText(tag);
            binding.tags.addView(pill);
        }

        bindPrice(event, now);

        // The main button: organizers read their feedback; attendees rate once the event is over;
        // otherwise register / check in.
        boolean ended = event.hasEnded(now);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        boolean isOrganizer = user != null && !event.getOrganizerId().isEmpty()
                && event.getOrganizerId().equals(user.getUid());
        if (isOrganizer) {
            binding.registerButton.setEnabled(true);
            binding.registerButton.setText(R.string.details_view_feedback);
            binding.registerButton.setOnClickListener(v ->
                    startActivity(FeedbackListActivity.createIntent(this, eventId)));
            return;
        }
        if (registered && ended) {
            binding.registerButton.setEnabled(true);
            binding.registerButton.setText(R.string.details_rate_event);
            binding.registerButton.setOnClickListener(v ->
                    startActivity(FeedbackActivity.createIntent(this, eventId)));
            return;
        }
        // Not registered: Register Now. Registered: the check-in ticket (until the event is over).
        binding.registerButton.setEnabled(!ended);
        binding.registerButton.setText(registered
                ? R.string.details_show_check_in
                : ended ? R.string.details_event_ended : R.string.details_register);
        binding.registerButton.setOnClickListener(v -> {
            if (registered) {
                startActivity(CheckInActivity.createIntent(this, eventId));
            } else {
                registrationLauncher.launch(RegistrationActivity.createIntent(this, eventId));
            }
        });
    }

    /** One price, or the early-bird price beside the regular rate while the offer lasts. */
    private void bindPrice(@NonNull Event event, long now) {
        if (event.isEarlyBirdActive(now)) {
            binding.priceMain.setText(EventFormat.priceText(this, event.getEarlyBirdPrice()));
            binding.priceMainCaption.setText(getString(R.string.details_price_early,
                    EventFormat.shortDateText(event.getEarlyBirdUntilMillis())));
            binding.priceSecond.setText(EventFormat.priceText(this, event.getPrice()));
            binding.priceSecondCaption.setText(R.string.details_price_regular);
            binding.priceSecondGroup.setVisibility(View.VISIBLE);
            binding.priceDivider.setVisibility(View.VISIBLE);
        } else {
            binding.priceMain.setText(EventFormat.priceText(this, event));
            binding.priceMainCaption.setText(event.isFree() ? R.string.details_price_free : R.string.details_price_fee);
            binding.priceSecondGroup.setVisibility(View.GONE);
            binding.priceDivider.setVisibility(View.GONE);
        }
    }

    /** The organizer's logo in a circle, or their initials when there is no logo. */
    private void bindOrganizerLogo(@NonNull Event event) {
        binding.organizerInitials.setText(initialsOf(event.getOrganizerName()));
        String logoUrl = event.getOrganizerLogoUrl();
        if (logoUrl == null) {
            Glide.with(binding.organizerLogo).clear(binding.organizerLogo);
            binding.organizerLogo.setVisibility(View.GONE);
            return;
        }
        binding.organizerLogo.setVisibility(View.VISIBLE);
        Glide.with(binding.organizerLogo)
                .load(logoUrl)
                .circleCrop()
                .into(binding.organizerLogo);
    }

    /** Hands the event to the phone's share sheet, so it can go to Facebook, Instagram, X, chats or email. */
    private void share(@NonNull Event event) {
        List<String> lines = new ArrayList<>();
        lines.add(getString(R.string.details_share_intro, event.getTitle()));
        String time = EventFormat.timeText(this, event);
        lines.add(EventFormat.detailDateText(event) + (time == null ? "" : ", " + time));
        String place = placeQuery(event);
        if (!place.isEmpty()) {
            lines.add(place);
        }
        lines.add(getString(R.string.details_share_outro));

        Intent send = new Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_SUBJECT, event.getTitle())
                .putExtra(Intent.EXTRA_TEXT, TextUtils.join("\n", lines));
        try {
            startActivity(Intent.createChooser(send, getString(R.string.details_share_chooser)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.details_share_failed, Toast.LENGTH_SHORT).show();
        }
    }

    /** Opens the venue in a maps app (or the browser if none is installed). */
    private void openMap(@NonNull Event event) {
        String query = placeQuery(event);
        if (query.isEmpty()) {
            return;
        }
        Intent geo = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(query)));
        try {
            startActivity(geo);
            return;
        } catch (ActivityNotFoundException ignored) {
            // No maps app: fall back to Google Maps in the browser.
        }
        Intent web = new Intent(Intent.ACTION_VIEW, Uri.parse(
                "https://www.google.com/maps/search/?api=1&query=" + Uri.encode(query)));
        try {
            startActivity(web);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.details_map_failed, Toast.LENGTH_SHORT).show();
        }
    }

    /** "Venue, address, city", skipping what is missing: the text a maps app searches for. */
    @NonNull
    private static String placeQuery(@NonNull Event event) {
        List<String> parts = new ArrayList<>();
        for (String part : new String[]{event.getVenue(), event.getAddress(), event.getCity()}) {
            if (!part.isEmpty() && !parts.contains(part)) {
                parts.add(part);
            }
        }
        return TextUtils.join(", ", parts);
    }

    private static void setTextOrHide(@NonNull TextView view, @Nullable String text) {
        boolean has = text != null && !text.isEmpty();
        view.setText(has ? text : "");
        view.setVisibility(has ? View.VISIBLE : View.GONE);
    }

    /** "Ateneo Innovation Center" becomes "AI"; a single word gives one letter. */
    @NonNull
    private static String initialsOf(@NonNull String name) {
        String[] words = name.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        if (words.length > 0 && !words[0].isEmpty()) {
            initials.appendCodePoint(words[0].codePointAt(0));
        }
        if (words.length > 1 && !words[1].isEmpty()) {
            initials.appendCodePoint(words[1].codePointAt(0));
        }
        return initials.toString().toUpperCase(Locale.getDefault());
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.detailsRoot, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }
}
