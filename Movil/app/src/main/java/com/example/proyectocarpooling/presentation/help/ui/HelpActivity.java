package com.example.proyectocarpooling.presentation.help.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyectocarpooling.CarPoolingApplication;
import com.example.proyectocarpooling.R;
import com.example.proyectocarpooling.data.local.SessionManager;
import com.example.proyectocarpooling.presentation.auth.ui.LoginActivity;
import com.example.proyectocarpooling.presentation.help.HelpFaqAdapter;
import com.example.proyectocarpooling.presentation.help.HelpFaqItem;

import java.util.ArrayList;
import java.util.List;

public class HelpActivity extends AppCompatActivity implements HelpFaqAdapter.Listener {

    private HelpFaqAdapter adapter;
    private List<HelpFaqItem> faqItems;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        SessionManager sessionManager = ((CarPoolingApplication) getApplication()).getSessionManager();
        if (!sessionManager.hasActiveSession()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.helpToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView recycler = findViewById(R.id.helpFaqRecycler);
        faqItems = buildFaqItems();
        adapter = new HelpFaqAdapter(this);
        adapter.setItems(faqItems);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        findViewById(R.id.helpFooterHint).setOnClickListener(v ->
                startActivity(new Intent(this, com.example.proyectocarpooling.presentation.support.ui.SupportActivity.class)));
    }

    @Override
    public void onQuestionToggled(int position) {
        if (position < 0 || position >= faqItems.size()) {
            return;
        }
        HelpFaqItem item = faqItems.get(position);
        if (item.getType() != HelpFaqItem.TYPE_QUESTION) {
            return;
        }
        item.setExpanded(!item.isExpanded());
        adapter.notifyItemChanged(position);
    }

    private List<HelpFaqItem> buildFaqItems() {
        List<HelpFaqItem> list = new ArrayList<>();

        list.add(HelpFaqItem.section(getString(R.string.help_section_reservations)));
        list.add(HelpFaqItem.question(
                getString(R.string.help_faq_find_driver_q),
                getString(R.string.help_faq_find_driver_a)));
        list.add(HelpFaqItem.question(
                getString(R.string.help_faq_reserve_q),
                getString(R.string.help_faq_reserve_a)));
        list.add(HelpFaqItem.question(
                getString(R.string.help_faq_cancel_reservation_q),
                getString(R.string.help_faq_cancel_reservation_a)));

        list.add(HelpFaqItem.section(getString(R.string.help_section_boarding)));
        list.add(HelpFaqItem.question(
                getString(R.string.help_faq_boarding_code_q),
                getString(R.string.help_faq_boarding_code_a)));
        list.add(HelpFaqItem.question(
                getString(R.string.help_faq_verify_boarding_q),
                getString(R.string.help_faq_verify_boarding_a)));

        list.add(HelpFaqItem.section(getString(R.string.help_section_trip)));
        list.add(HelpFaqItem.question(
                getString(R.string.help_faq_chat_q),
                getString(R.string.help_faq_chat_a)));
        list.add(HelpFaqItem.question(
                getString(R.string.help_faq_ratings_q),
                getString(R.string.help_faq_ratings_a)));
        list.add(HelpFaqItem.question(
                getString(R.string.help_faq_driver_trip_q),
                getString(R.string.help_faq_driver_trip_a)));

        list.add(HelpFaqItem.section(getString(R.string.help_section_account)));
        list.add(HelpFaqItem.question(
                getString(R.string.help_faq_register_q),
                getString(R.string.help_faq_register_a)));
        list.add(HelpFaqItem.question(
                getString(R.string.help_faq_favorites_q),
                getString(R.string.help_faq_favorites_a)));

        return list;
    }
}
