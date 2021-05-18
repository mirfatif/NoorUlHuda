package com.mirfatif.noorulhuda.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface TagAayahsDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  void create(TagAayahsEntity entity);

  @Query("SELECT aayahId FROM TagAayahsEntity WHERE tagId = :tagId")
  List<Integer> getAayahIds(int tagId);

  @Query("DELETE FROM TagAayahsEntity WHERE tagId = :tagId")
  void remove(int tagId);

  @Query("DELETE FROM TagAayahsEntity WHERE tagId = :tagId AND aayahId = :aayahId")
  void remove(int tagId, int aayahId);

  @Query("DELETE FROM TagAayahsEntity WHERE tagId = :tagId AND aayahId IN (:aayahIds)")
  void remove(int tagId, List<Integer> aayahIds);
}
