package com.example.aromaatlas;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class TableViewHolder extends RecyclerView.ViewHolder {
    TextView txtTableNo, txtCapacity;
    Button btnEdit, btnDelete;

    public TableViewHolder(@NonNull View itemView) {
        super(itemView);
        txtTableNo = itemView.findViewById(R.id.txtTableNo);
        txtCapacity = itemView.findViewById(R.id.txtCapacity);
        btnEdit = itemView.findViewById(R.id.btnEdit);
        btnDelete = itemView.findViewById(R.id.btnDelete);
    }

    public void bind(TableModel model) {
        txtTableNo.setText("Table " + model.getTableNo());
        txtCapacity.setText("Seats: " + model.getCapacity());
    }
}
