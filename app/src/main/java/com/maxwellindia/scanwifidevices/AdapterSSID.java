package com.maxwellindia.scanwifidevices;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import java.util.ArrayList;

public class AdapterSSID extends RecyclerView.Adapter<AdapterSSID.SSIDHolder> {

    private Context context;
    private ArrayList<ModalSSID> ssidList;
    private OnItemClickListener onItemClickListener;

    public AdapterSSID(Context context, ArrayList<ModalSSID> ssidList) {
        this.context = context;
        this.ssidList = ssidList;
    }


    @NonNull
    @Override
    public SSIDHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.card_scan_sevice, parent, false);
        return new SSIDHolder(view, onItemClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull SSIDHolder holder, int position) {
        ModalSSID modalSSID = ssidList.get(position);

        holder.tvSSID.setText(modalSSID.getSsid());
    }

    @Override
    public int getItemCount() {
        return ssidList.size();
    }

    public static class SSIDHolder extends RecyclerView.ViewHolder {

        private TextView tvSSID;

        public SSIDHolder(@NonNull View itemView, final OnItemClickListener listener) {
            super(itemView);

            tvSSID = itemView.findViewById(R.id.tv_ssid_scan);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onItemClick(position);
                        }
                    }
                }
            });

        }

    }

    public interface OnItemClickListener {
        void onItemClick(int positon);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        onItemClickListener = listener;
    }

}
