package com.mirfatif.noorulhuda.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class SurahEntity {

  public SurahEntity(
      int surahNum, int aayahs, int rukus, int order, String name, boolean isMeccan) {
    this.surahNum = surahNum;
    this.aayahs = aayahs;
    this.rukus = rukus;
    this.order = order;
    this.name = name;
    this.isMeccan = isMeccan;
  }

  @PrimaryKey public int surahNum;

  public int aayahs, rukus, order;

  public String name;

  public boolean isMeccan;
}
