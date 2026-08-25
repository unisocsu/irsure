package kernel.unisocsu.irsure.ui;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import kernel.unisocsu.irsure.R;
import kernel.unisocsu.irsure.models.AcCodeset;

/**
 * NOTE: uses android.support.v7 (old Support Library) to match "API 19+ / simple setup".
 * If your project already migrated to AndroidX, replace the android.support.v7.* and
 * android.support.annotation.NonNull imports with their androidx.* equivalents -
 * the rest of the class is identical either way.
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
