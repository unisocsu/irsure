package kernel.unisocsu.irsure.ui;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import kernel.unisocsu.irsure.R;
import kernel.unisocsu.irsure.models.AcCodeset;

/**
 * Uses AndroidX (androidx.recyclerview / androidx.annotation).
 */
public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder> {

    public interface OnDeviceClickListener {
        void onDeviceClick(AcCodeset codeset);
    }

    private final List<AcCodeset> items = new ArrayList<>();
    private final OnDeviceClickListener listener;

    public DeviceAdapter(OnDeviceClickListener listener) {
        this.listener = listener;
    }

    /** Convenience constructor used by the scan results screen. */
    public DeviceAdapter(List<AcCodeset> initialItems, OnDeviceClickListener listener) {
        this.listener = listener;
        if (initialItems != null) {
            this.items.addAll(initialItems);
        }
    }

    /** Replaces the full list (called after each search/filter query). */
    public void submitList(List<AcCodeset> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AcCodeset codeset = items.get(position);
        holder.title.setText(codeset.getBrands() == null || codeset.getBrands().isEmpty()
                ? codeset.getName() : codeset.getBrands());
        holder.subtitle.setText(codeset.getName());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onDeviceClick(codeset);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView subtitle;

        ViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.text_device_title);
            subtitle = itemView.findViewById(R.id.text_device_subtitle);
        }
    }
}
