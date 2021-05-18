package com.mirfatif.noorulhuda.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = TagEntity.class, version = 1, exportSchema = false)
public abstract class TagsDatabase extends RoomDatabase {

  public abstract TagsDao getDao();
}
