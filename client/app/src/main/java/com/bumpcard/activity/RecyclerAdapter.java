package com.bumpcard.activity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumpcard.R;

import java.util.ArrayList;

public class RecyclerAdapter  extends RecyclerView.Adapter<RecyclerAdapter.MyViewHolder> {
    private ArrayList<BusinessCard> businessCardsList;

    public RecyclerAdapter(ArrayList<BusinessCard> businessCardsList) {
        this.businessCardsList = businessCardsList;
    }

    public class MyViewHolder extends RecyclerView.ViewHolder{
        private TextView name, surname, phone, email;

        public MyViewHolder(final View view){
            super(view);
            name = view.findViewById(R.id.name);
            surname = view.findViewById(R.id.surname);
            phone = view.findViewById(R.id.phone);
            email = view.findViewById(R.id.email);
        }

        public TextView getName() {
            return name;
        }

        public void setName(TextView name) {
            this.name = name;
        }

        public TextView getSurname() {
            return surname;
        }

        public void setSurname(TextView surname) {
            this.surname = surname;
        }

        public TextView getPhone() {
            return phone;
        }

        public void setPhone(TextView phone) {
            this.phone = phone;
        }

        public TextView getEmail() {
            return email;
        }

        public void setEmail(TextView email) {
            this.email = email;
        }
    }

    @NonNull
    @Override
    public RecyclerAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.business_card, parent, false);

        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerAdapter.MyViewHolder holder, int position) {
        BusinessCard businessCard = businessCardsList.get(position);
        String name = businessCard.getName();
        String surname = businessCard.getSurname();
        String phone = businessCard.getPhone();
        String email = businessCard.getEmail();

        holder.name.setText(name);
        holder.surname.setText(surname);
        holder.phone.setText(phone);
        holder.email.setText(email);
    }

    @Override
    public int getItemCount() {
        return businessCardsList.size();
    }
}
