/*
 * This file is part of Grocy Android.
 *
 * Grocy Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Grocy Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020-2021 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.repository;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import androidx.preference.PreferenceManager;
import java.util.ArrayList;
import xyz.zedler.patrick.grocy.database.AppDatabase;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.util.Constants.PREF;

public class StorageRepository {

  private final AppDatabase appDatabase;
  private final SharedPreferences sharedPrefs;

  public StorageRepository(Application application) {
    this.appDatabase = AppDatabase.getAppDatabase(application);
    this.sharedPrefs = PreferenceManager.getDefaultSharedPreferences(application);
  }

  public void storeStockItems(ArrayList<StockItem> stockItems, String dbChangedTime, Runnable onFinished) {
    StoreInDatabaseAsync storeAsync = new StoreInDatabaseAsync(appDatabase, sharedPrefs, dbChangedTime, onFinished);
    storeAsync.stockItems = stockItems;
    storeAsync.execute();
  }

  public void storeShoppingListItems(ArrayList<ShoppingListItem> shoppingListItems, String dbChangedTime, Runnable onFinished) {
    StoreInDatabaseAsync store = new StoreInDatabaseAsync(appDatabase, sharedPrefs, dbChangedTime, onFinished);
    store.shoppingListItems = shoppingListItems;
    store.execute();
  }

  public void storeProducts(ArrayList<Product> products, String dbChangedTime, Runnable onFinished) {
    StoreInDatabaseAsync store = new StoreInDatabaseAsync(appDatabase, sharedPrefs, dbChangedTime, onFinished);
    store.products = products;
    store.execute();
  }

  private static class StoreInDatabaseAsync extends AsyncTask<Void, Void, Void> {

    private final AppDatabase appDatabase;
    private final SharedPreferences sharedPrefs;
    private ArrayList<StockItem> stockItems;
    private ArrayList<ShoppingListItem> shoppingListItems;
    private ArrayList<Product> products;
    private final String dbChangedTime;
    private final Runnable onFinished;

    StoreInDatabaseAsync(
        AppDatabase appDatabase,
        SharedPreferences sharedPrefs,
        String dbChangedTime,
        Runnable onFinished
    ) {
      this.appDatabase = appDatabase;
      this.sharedPrefs = sharedPrefs;
      this.dbChangedTime = dbChangedTime;
      this.onFinished = onFinished;
    }

    @Override
    protected final Void doInBackground(Void... params) {
      SharedPreferences.Editor editPrefs = sharedPrefs.edit();
      if (stockItems != null) {
        appDatabase.stockItemDao().deleteAll();
        appDatabase.stockItemDao().insertAll(stockItems);
        editPrefs.putString(PREF.DB_LAST_TIME_STOCK_ITEMS, dbChangedTime);
      }
      if (shoppingListItems != null) {
        appDatabase.shoppingListItemDao().deleteAll();
        appDatabase.shoppingListItemDao().insertAll(shoppingListItems);
        editPrefs.putString(PREF.DB_LAST_TIME_SHOPPING_LIST_ITEMS, dbChangedTime);
      }
      if (products != null) {
        appDatabase.productDao().deleteAll();
        appDatabase.productDao().insertAll(products);
        editPrefs.putString(PREF.DB_LAST_TIME_PRODUCTS, dbChangedTime);
      }
      editPrefs.apply();
      return null;
    }

    @Override
    protected void onPostExecute(Void unused) {
      if (onFinished != null) onFinished.run();
    }
  }
}
