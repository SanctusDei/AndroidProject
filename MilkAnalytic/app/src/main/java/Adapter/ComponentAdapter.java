package utils;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.ubi.NanoScan.databinding.ItemComponentBinding;
import java.util.List;

public class ComponentAdapter extends RecyclerView.Adapter<ComponentAdapter.ViewHolder> {
    private final List<ComponentItem> items;

    public ComponentAdapter(List<ComponentItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemComponentBinding binding = ItemComponentBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ComponentItem item = items.get(position);
        holder.binding.tvCompLabel.setText(item.label);
        holder.binding.tvCompValue.setText(item.value);
        holder.binding.tvCompUnit.setText(item.unit);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemComponentBinding binding;
        ViewHolder(ItemComponentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}