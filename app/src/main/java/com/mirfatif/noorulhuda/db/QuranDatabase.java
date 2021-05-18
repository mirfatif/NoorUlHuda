package com.mirfatif.noorulhuda.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = AayahEntity.class, version = 1, exportSchema = false)
public abstract class QuranDatabase extends RoomDatabase {

  public abstract QuranDao getDao();
}
