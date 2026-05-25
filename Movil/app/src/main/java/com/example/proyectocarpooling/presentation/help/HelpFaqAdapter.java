package com.example.proyectocarpooling.presentation.help;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyectocarpooling.R;

import java.util.ArrayList;
import java.util.List;

public class HelpFaqAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface Listener {
        void onQuestionToggled(int position);
    }

    private final List<HelpFaqItem> items = new ArrayList<>();
    private final Listener listener;

    public HelpFaqAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<HelpFaqItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == HelpFaqItem.TYPE_SECTION) {
            View view = inflater.inflate(R.layout.item_section_header, parent, false);
            return new SectionViewHolder(view);
        }
        View view = inflater.inflate(R.layout.item_help_faq, parent, false);
        return new QuestionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        HelpFaqItem item = items.get(position);
        if (holder instanceof SectionViewHolder) {
            ((SectionViewHolder) holder).bind(item.getSectionTitle());
            return;
        }
        QuestionViewHolder questionHolder = (QuestionViewHolder) holder;
        questionHolder.bind(item);
        questionHolder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onQuestionToggled(holder.getBindingAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class SectionViewHolder extends RecyclerView.ViewHolder {
        private final TextView title;

        SectionViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.sectionHeaderText);
        }

        void bind(String sectionTitle) {
            title.setText(sectionTitle);
        }
    }

    static final class QuestionViewHolder extends RecyclerView.ViewHolder {
        private final TextView question;
        private final TextView answer;
        private final ImageView expandIcon;

        QuestionViewHolder(@NonNull View itemView) {
            super(itemView);
            question = itemView.findViewById(R.id.helpFaqQuestion);
            answer = itemView.findViewById(R.id.helpFaqAnswer);
            expandIcon = itemView.findViewById(R.id.helpFaqExpandIcon);
        }

        void bind(HelpFaqItem item) {
            question.setText(item.getQuestion());
            answer.setText(item.getAnswer());
            boolean expanded = item.isExpanded();
            answer.setVisibility(expanded ? View.VISIBLE : View.GONE);
            expandIcon.setRotation(expanded ? 180f : 0f);
        }
    }
}
