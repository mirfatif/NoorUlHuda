package com.mirfatif.noorulhuda.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class TagAayahsEntity {

  public TagAayahsEntity(int tagId, int aayahId) {
    this.tagId = tagId;
    this.aayahId = aayahId;
  }

  @PrimaryKey(autoGenerate = true)
  public int id;

  public int tagId, aayahId;
}
