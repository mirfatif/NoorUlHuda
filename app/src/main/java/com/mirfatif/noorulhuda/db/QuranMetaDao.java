package com.mirfatif.noorulhuda.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface QuranMetaDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  void insert(SurahEntity entity);

  @Query("SELECT aayahs FROM SurahEntity WHERE surahNum = :surahNum")
  int getAayahs(int surahNum);

  @Query("SELECT * FROM SurahEntity WHERE surahNum = :surahNum")
  SurahEntity getSurah(int surahNum);

  @Query("SELECT name FROM SurahEntity WHERE surahNum = :surahNum")
  String getSurahName(int surahNum);
}
