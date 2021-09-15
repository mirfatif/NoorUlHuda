package com.mirfatif.noorulhuda.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class AayahEntity {

  public AayahEntity(int id, int surahNum, int aayahNum) {
    this.id = id;
    this.surahNum = surahNum;
    this.aayahNum = aayahNum;
  }

  @PrimaryKey public int id;

  public String text;

  public int surahNum, aayahNum;
  public int juz, manzil, page;
  public boolean juzStarts = false,
      manzilStarts = false,
      rukuEnds = false,
      hizbEnds = false,
      hasSajda = false;
  public int aayahGroupPos, aayahGroupPosInPage;
}
