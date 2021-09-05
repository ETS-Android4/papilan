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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) 2020-2021 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.repository;

import android.app.Application;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.Action;
import io.reactivex.rxjava3.functions.Function8;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.List;
import xyz.zedler.patrick.grocy.database.AppDatabase;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductBarcode;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.model.StockLocation;

public class StockOverviewRepository {

  private final AppDatabase appDatabase;
  private final Scheduler dbScheduler;
  private final Scheduler mainScheduler;

  public StockOverviewRepository(Application application) {
    this.appDatabase = AppDatabase.getAppDatabase(application);
    this.dbScheduler = Schedulers.from(appDatabase.getQueryExecutor());
    this.mainScheduler = AndroidSchedulers.mainThread();
  }

  public void load(StockOverviewDataListener listener) {
    Single.zip(
        appDatabase.quantityUnitDao().getAllRxSingle(),
        appDatabase.productGroupDao().getAllRxSingle(),
        appDatabase.stockItemDao().getAllRxSingle(),
        appDatabase.productDao().getAllRxSingle(),
        appDatabase.productBarcodeDao().getAllRxSingle(),
        appDatabase.shoppingListItemDao().getAllRxSingle(),
        appDatabase.locationDao().getAllRxSingle(),
        appDatabase.stockLocationDao().getAllRxSingle(),
        StockOverViewData.getZipFunction()
    ).subscribeOn(dbScheduler).observeOn(mainScheduler)
        .doOnSuccess(listener::onFinished).subscribe();
  }

  public static class StockOverViewData {
    public List<QuantityUnit> quantityUnits;
    public List<ProductGroup> productGroups;
    public List<StockItem> stockItems;
    public List<Product> products;
    public List<ProductBarcode> productBarcodes;
    public List<ShoppingListItem> shoppingListItems;
    public List<Location> locations;
    public List<StockLocation> stockLocations;

    public static Function8<List<QuantityUnit>, List<ProductGroup>, List<StockItem>,
        List<Product>, List<ProductBarcode>, List<ShoppingListItem>, List<Location>,
        List<StockLocation>, StockOverViewData> getZipFunction() {
      return (quantityUnits, productGroups, stockItems, products, barcodes, shoppingListItems,
          locations, stockLocations) -> {
        StockOverViewData data = new StockOverViewData();
        data.quantityUnits = quantityUnits;
        data.productGroups = productGroups;
        data.stockItems = stockItems;
        data.products = products;
        data.productBarcodes = barcodes;
        data.shoppingListItems = shoppingListItems;
        data.locations = locations;
        data.stockLocations = stockLocations;
        return data;
      };
    }
  }

  public interface StockOverviewDataListener {
    void onFinished(StockOverViewData data);
  }

  public void store(
      List<QuantityUnit> q, List<ProductGroup> pg, List<StockItem> si, List<Product> p,
      List<ProductBarcode> pb, List<ShoppingListItem> sli, List<Location> l,
      List<StockLocation> sl, Action onFinished
  ) {
    appDatabase.quantityUnitDao().deleteAllRx()
        .andThen(appDatabase.quantityUnitDao().deleteAllRx())
        .andThen(appDatabase.quantityUnitDao().insertAllRx(q))
        .andThen(appDatabase.productGroupDao().deleteAllRx())
        .andThen(appDatabase.productGroupDao().insertAllRx(pg))
        .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(onFinished);
  }
}
