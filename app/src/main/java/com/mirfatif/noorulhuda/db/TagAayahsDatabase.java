package com.mirfatif.noorulhuda.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = TagAayahsEntity.class, version = 1, exportSchema = false)
public abstract class TagAayahsDatabase extends RoomDatabase {

  public abstract TagAayahsDao getDao();
}
