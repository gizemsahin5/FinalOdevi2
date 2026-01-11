package com.gizemsahin.finalodevi;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    EditText etUrun, etMiktar;
    Spinner spBirim;
    Button btnEkle;
    ListView lvListe;

    ArrayList<String> urunListesi;
    ArrayAdapter<String> adapter;
    DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        // View tanımlamaları
        etUrun = findViewById(R.id.etUrun);
        etMiktar = findViewById(R.id.etMiktar);
        spBirim = findViewById(R.id.spBirim);
        btnEkle = findViewById(R.id.btnEkle);
        lvListe = findViewById(R.id.lvListe);

        // Spinner verileri
        String[] birimler = {"adet", "kg"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                birimler
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spBirim.setAdapter(spinnerAdapter);

        // Liste oluşturma
        urunListesi = new ArrayList<>();
        adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                urunListesi
        );
        lvListe.setAdapter(adapter);

        // Veritabanındaki ürünleri ListView'a yükle
        Cursor cursor = dbHelper.getAllProducts();
        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(cursor.getColumnIndexOrThrow("isim"));
                String quantity = cursor.getString(cursor.getColumnIndexOrThrow("miktar"));
                String display = name;
                if (!quantity.isEmpty()) {
                    display += " (" + quantity + ")";
                }
                urunListesi.add(display);
            } while (cursor.moveToNext());
        }
        cursor.close();
        adapter.notifyDataSetChanged();

        // EKLE BUTONU
        btnEkle.setOnClickListener(v -> {
            String urun = etUrun.getText().toString().trim();
            String miktar = etMiktar.getText().toString().trim();
            String birim = spBirim.getSelectedItem().toString();

            if (urun.isEmpty()) {
                Toast.makeText(MainActivity.this,
                        "Ürün adı boş olamaz",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            String yeniUrun;
            if (miktar.isEmpty()) {
                yeniUrun = urun;
            } else {
                yeniUrun = urun + " (" + miktar + " " + birim + ")";
            }

            // Veritabanına kaydet
            boolean inserted = dbHelper.addProduct(urun, miktar.isEmpty() ? "" : miktar + " " + birim);
            if (inserted) {
                Toast.makeText(MainActivity.this,
                        "Ürün eklendi",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this,
                        "Hata oluştu",
                        Toast.LENGTH_SHORT).show();
            }

            urunListesi.add(yeniUrun);
            adapter.notifyDataSetChanged();

            etUrun.setText("");
            etMiktar.setText("");
        });

        // SİLME (uzun bas)
        lvListe.setOnItemLongClickListener((parent, view, position, id) -> {
            // Önce DB'den sil
            String display = urunListesi.get(position);
            String urunAdi = display.contains("(") ? display.substring(0, display.indexOf(" (")) : display;
            dbHelper.deleteProduct(urunAdi);

            urunListesi.remove(position);
            adapter.notifyDataSetChanged();
            Toast.makeText(MainActivity.this, "Ürün silindi", Toast.LENGTH_SHORT).show();
            return true;
        });
    }

    // DatabaseHelper sınıfı
    public static class DatabaseHelper extends SQLiteOpenHelper {

        private static final String DATABASE_NAME = "alisveris.db";
        private static final int DATABASE_VERSION = 1;

        private static final String TABLE_NAME = "urunler";
        private static final String COL_ID = "id";
        private static final String COL_NAME = "isim";
        private static final String COL_QUANTITY = "miktar";

        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + "("
                    + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COL_NAME + " TEXT,"
                    + COL_QUANTITY + " TEXT)";
            db.execSQL(CREATE_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(db);
        }

        // Veri ekleme
        public boolean addProduct(String name, String quantity) {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COL_NAME, name);
            values.put(COL_QUANTITY, quantity);
            long result = db.insert(TABLE_NAME, null, values);
            db.close();
            return result != -1;
        }

        // Verileri listeleme
        public Cursor getAllProducts() {
            SQLiteDatabase db = this.getReadableDatabase();
            return db.rawQuery("SELECT * FROM " + TABLE_NAME, null);
        }

        // Veriyi silme
        public void deleteProduct(String name) {
            SQLiteDatabase db = this.getWritableDatabase();
            db.delete(TABLE_NAME, COL_NAME + "=?", new String[]{name});
            db.close();
        }
    }
}
