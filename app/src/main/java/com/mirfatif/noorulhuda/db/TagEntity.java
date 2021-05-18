package com.mirfatif.noorulhuda.db;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import java.util.HashSet;
import java.util.Set;

@Entity
public class TagEntity {

  public TagEntity() {
    timeStamp = System.currentTimeMillis();
  }

  @PrimaryKey(autoGenerate = true)
  public int id;

  public long timeStamp;

  public String title, desc;

  @Ignore public final Set<Integer> aayahIds = new HashSet<>();

  @Ignore public int surahCount;
}
