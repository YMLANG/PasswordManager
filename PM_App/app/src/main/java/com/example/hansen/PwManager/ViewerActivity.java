package com.example.hansen.PwManager;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class ViewerActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;

    private RecyclerView mItemList;
    private DatabaseReference mDatabase;
    private FloatingActionButton fab;
    private Toolbar toolbar;
    private ArrayList<Account> acc_list;
    private static final String key = "This is a key123";
    private static final String iv = "This is an IV123";
    private boolean deleteConfirm = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewer);

        acc_list = new ArrayList<>();
        mAuth = FirebaseAuth.getInstance();
        String user_id = mAuth.getCurrentUser().getUid();

        mDatabase=FirebaseDatabase.getInstance().getReference().child("users").child(user_id);
        mDatabase.keepSynced(true);

        mItemList=(RecyclerView)findViewById(R.id.myrecycleview);
        mItemList.setHasFixedSize(true);
        mItemList.setLayoutManager(new LinearLayoutManager(this));

        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ViewerActivity.this, AddDetailsActivity.class));
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_items, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch(id){
            case R.id.logout_menu:
                onBackPressed();
                break;
        }
        return true;
    }

    public static interface ClickListener {
        public void onClick(View view,int position);
        public void onLongClick(View view, int position);
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(ViewerActivity.this);
        builder.setMessage("Are you sure want to exit")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mAuth.signOut();
                        ViewerActivity.this.finish();
                        startActivity(new Intent(ViewerActivity.this, MainActivity.class));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    protected void onStart() {


        super.onStart();
        final FirebaseRecyclerAdapter<Account, AccountViewHolder>firebaseRecyclerAdapter=new FirebaseRecyclerAdapter<Account, AccountViewHolder>
                (Account.class, R.layout.item_layout_post, AccountViewHolder.class, mDatabase) {
            @Override
            protected void populateViewHolder(AccountViewHolder viewHolder, Account model, int position) {
                final DatabaseReference ref = getRef(position);
                final String platform = ref.getKey();

                String decryptedAcc = model.getAccount();
                String decryptedPass = model.getPassword();
                try {
                    decryptedAcc = decrypt(decryptedAcc);
                    decryptedAcc = decryptedAcc.replaceAll("\\{*$", "");
                    decryptedPass = decrypt(decryptedPass);
                    decryptedPass = decryptedPass.replaceAll("\\{*$", "");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                viewHolder.setUsername(decryptedAcc);
                viewHolder.setPassword(decryptedPass);
                viewHolder.setPlatform(platform);
                viewHolder.setImage(platform);

                final int total_count = getItemCount();
                final int item_counter = 0;
            }
        };
        mItemList.setAdapter(firebaseRecyclerAdapter);

        mDatabase.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                final long count = dataSnapshot.getChildrenCount();

                ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0,
                        ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
                    @Override
                    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                          RecyclerView.ViewHolder target) {
                        Toast.makeText(ViewerActivity.this, "on Move", Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    @Override
                    public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                        AlertDialog.Builder test = deleteDialog();
                        test.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                             @Override
                             public void onClick(DialogInterface dialog, int which) {
                                 deleteConfirm = true;
                             }
                        });
                        test.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                deleteConfirm = false;
                            }
                        });
                        if (deleteConfirm == true) {
                            Toast.makeText(ViewerActivity.this, "Account Deleted ", Toast.LENGTH_SHORT).show();
                            int position = viewHolder.getAdapterPosition();
                            String user_id = mAuth.getCurrentUser().getUid();
                            firebaseRecyclerAdapter.getRef(position).removeValue();
                            firebaseRecyclerAdapter.notifyItemRemoved(position);

                            if (count == 1) {
                                mDatabase.getParent().child(user_id).removeValue();
                            }

                            mDatabase.child(firebaseRecyclerAdapter.getRef(position).getKey().toString().trim()).removeValue();
                            firebaseRecyclerAdapter.notifyDataSetChanged();
                        }
                        AlertDialog deleting = test.create();
                        deleting.show();


                    }
                };

                ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
                itemTouchHelper.attachToRecyclerView(mItemList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        mItemList.addOnItemTouchListener(new RecyclerTouchListener(this, mItemList, new ClickListener() {

            @Override
            public void onClick(View view, final int position) {

            }

            @Override
            public void onLongClick(View view, int position) {

                final EditText input_accName;
                final EditText input_password;
                final DatabaseReference ref = firebaseRecyclerAdapter.getRef(position);
                final String platform = ref.getKey();
                Toast.makeText(ViewerActivity.this, "Long press on position :" + position,
                        Toast.LENGTH_LONG).show();
                AlertDialog.Builder builder = new AlertDialog.Builder(ViewerActivity.this);

                LinearLayout ll = new LinearLayout(ViewerActivity.this);
                ll.setOrientation(LinearLayout.VERTICAL);

                input_accName = new EditText(ViewerActivity.this);
                input_accName.setHint("New Account Name");
                input_password = new EditText(ViewerActivity.this);
                input_password.setHint("New Password");

//                builder.setMessage("New Account Name");
//                builder.setView(input_accName);
//                builder.setMessage("New Password");
//                builder.setView(input_password);
                ll.addView(input_accName);
                builder.setMessage("Edit Account details");
                ll.addView(input_password);
                builder.setView(ll);

                builder.setPositiveButton("Submit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.e("new acc name:", input_accName.getText().toString());
                        Log.e("new password:", input_password.getText().toString());
                        String str_acc = input_accName.getText().toString();
                        String str_pass = input_password.getText().toString();

                        mDatabase.child(platform).child("account").setValue(str_acc);
                        mDatabase.child(platform).child("password").setValue(str_pass);

                        firebaseRecyclerAdapter.notifyDataSetChanged();
                    }
                })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });

                AlertDialog alert = builder.create();
                alert.show();
                TextView textView = (TextView) alert.findViewById(android.R.id.message);
                textView.setTextSize(20);
            }
        }));

    }

    public static class AccountViewHolder extends RecyclerView.ViewHolder {
        View mView;
        public AccountViewHolder(View itemView){
            super(itemView);
            mView=itemView;
        }

        public void setUsername(String s){
            TextView item_username=(TextView)mView.findViewById(R.id.item_username);
            item_username.setText(s);
        }

        public void setPassword(String s){
            TextView item_password = (TextView)mView.findViewById(R.id.item_password);
            item_password.setText(s);
        }

        public void setPlatform(String s){
            TextView item_platform= (TextView)mView.findViewById(R.id.item_platform);
            item_platform.setText(s);
        }
        public void setImage(String s){
            ImageView item_image = (ImageView)mView.findViewById(R.id.item_icon);
            if (s.equals("Facebook")) item_image.setImageResource(R.drawable.facebook);
            else if(s.equals("Twitter")) item_image.setImageResource(R.drawable.twitter);
            else if(s.equals("Amazon")) item_image.setImageResource(R.drawable.amazon);
            else if(s.equals("Instagram")) item_image.setImageResource(R.drawable.instagram);
            else if(s.equals("Steam")) item_image.setImageResource(R.drawable.steam);
            else if(s.equals("Apple")) item_image.setImageResource(R.drawable.apple);
            else if(s.equals("Microsoft")) item_image.setImageResource(R.drawable.microsoft);
            else item_image.setImageResource(R.drawable.internet);

        }
    }

    public AlertDialog.Builder deleteDialog() {
        AlertDialog.Builder deleteDialog = new AlertDialog.Builder(this);
        deleteDialog.setTitle("Delete");
        deleteDialog.setMessage("Are you sure you want to delete?");
        return deleteDialog;
    }

    private String decrypt(String encryptedDetail) throws Exception{
        Cipher c = Cipher.getInstance("AES/CBC/NoPadding");
        SecretKeySpec sKeySpec = new SecretKeySpec(key.getBytes(), "AES");
        c.init(Cipher.DECRYPT_MODE, sKeySpec, new IvParameterSpec(iv.getBytes()));
        byte[] decodedDetail = Base64.decode(encryptedDetail, Base64.DEFAULT);
        byte[] decDetail = c.doFinal(decodedDetail);
        String decryptedDetail = new String(decDetail);

        return decryptedDetail;
    }
}
