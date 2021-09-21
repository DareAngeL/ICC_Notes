package modules_list_activity_classes;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.tajos.iccnotes.ModuleContentActivity;
import com.tajos.iccnotes.R;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import iccnote.App;
import iccnote.Module;
import layouts.RoundedLayout;

public class ModuleListAdapter extends RecyclerView.Adapter<ModuleListAdapter.ViewHolder> {
    private final List<Module> data;
    private static HashMap<String, Object> intentData;
    private final Context context;

    public OnModuleClickedListener mListener = null;
    public interface OnModuleClickedListener {
        void onDeleteBtnClick(final int position);
        void onClick();
    }

    public void setOnModuleClickedListener(final OnModuleClickedListener listener) {
        mListener = listener;
    }

    public ModuleListAdapter(final Context cn, final List<Module> data, final HashMap<String, Object> intentData) {
        context = cn;
        this.data = data;
        ModuleListAdapter.intentData = intentData;
    }

    @NonNull
    @Override
    public ModuleListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View _view = LayoutInflater.from(parent.getContext()).inflate(R.layout.module_list_fragment, parent, false);
        return new ViewHolder(_view);
    }

    private boolean isOnDeleteMode = false;
    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        View _view = holder.itemView;
        final RoundedLayout mainRoot = _view.findViewById(R.id.main_root);
        final TextView moduleName = _view.findViewById(R.id.module_name);
        final ImageView deleteIcon = _view.findViewById(R.id.delete_ic);

        final String key = App.getKey(data.get(position));
        moduleName.setText(key);

        /* * * * * * * * * *
        * LISTENERS BELOW
        * * * * * * * * * */
        // mainRoot on click
        mainRoot.setOnClickListener(view -> {
            if (isOnDeleteMode) {
                //turn to => is not on delete mode
                deleteIcon.setVisibility(View.GONE);
                mainRoot.setBgroundColor(ResourcesCompat.getColor(context.getResources(), R.color.toolbarColor, null));
                isOnDeleteMode = false;
                return;
            }

            final Intent intent = new Intent(context, ModuleContentActivity.class);
            if (Objects.equals(data.get(position).get(key), "null")) {
                mListener.onClick();
                final String jsonStringModule = new Gson().toJson(data);
                intent.putExtra("modules", jsonStringModule);
                intent.putExtra("contents", "null");
                intent.putExtra("subject_key", Objects.requireNonNull(intentData.get("key")).toString());
                intent.putExtra("module_key", key);
                intent.putExtra("term", Objects.requireNonNull(intentData.get("term")).toString());
                intent.putExtra("subject_position", Integer.parseInt(Objects.requireNonNull(intentData.get("position")).toString()));
                intent.putExtra("module_position", position);
                context.startActivity(intent);
                return;
            }
            mListener.onClick();
            final String jsonStringContent = new Gson().toJson(data.get(position).get(key));
            final String jsonStringModule = new Gson().toJson(data);
            intent.putExtra("modules", jsonStringModule);
            intent.putExtra("contents", jsonStringContent);
            intent.putExtra("subject_key", Objects.requireNonNull(intentData.get("key")).toString());
            intent.putExtra("module_key", key);
            intent.putExtra("term", Objects.requireNonNull(intentData.get("term")).toString());
            intent.putExtra("subject_position", Integer.parseInt(Objects.requireNonNull(intentData.get("position")).toString()));
            intent.putExtra("module_position", position);
            context.startActivity(intent);
        });
        // mainRoot on long click
        if (position != data.size()-1 || key.equals(Module.FIRST_MEETING)) {
            mainRoot.setOnLongClickListener(view -> {
                // is on delete mode
                if (!isOnDeleteMode) {
                    deleteIcon.setVisibility(View.VISIBLE);
                    mainRoot.setBgroundColor(ResourcesCompat.getColor(context.getResources(), R.color.delete_bg_clor, null));
                    isOnDeleteMode = true;
                    return isOnDeleteMode;
                }
                // is not on delete mode
                deleteIcon.setVisibility(View.GONE);
                mainRoot.setBgroundColor(ResourcesCompat.getColor(context.getResources(), R.color.toolbarColor, null));
                isOnDeleteMode = false;
                return true;
            });
        }
        // on delete icon clicked
        deleteIcon.setOnClickListener(view -> {
            if (position == data.size()-2 || key.equals(Module.FIRST_MEETING)) {
                mListener.onDeleteBtnClick(position);
                deleteIcon.setVisibility(View.GONE);
                mainRoot.setBgroundColor(ResourcesCompat.getColor(context.getResources(), R.color.toolbarColor, null));
                this.notifyDataSetChanged();
                isOnDeleteMode = false;
                return;
            }
            Toast.makeText(context, "please delete the last module first!", Toast.LENGTH_SHORT).show();
            deleteIcon.setVisibility(View.GONE);
            mainRoot.setBgroundColor(ResourcesCompat.getColor(context.getResources(), R.color.toolbarColor, null));
            isOnDeleteMode = false;
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
