package com.mirfatif.noorulhuda.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = SurahEntity.class, version = 1, exportSchema = false)
public abstract class QuranMetaDatabase extends RoomDatabase {

  public abstract QuranMetaDao getDao();
}
