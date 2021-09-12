package com.mirfatif.noorulhuda.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Dao
public interface QuranDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  void insertAll(List<AayahEntity> entities);

  @Query(
      "UPDATE AayahEntity SET juz=:juz WHERE (:minSurah != :maxSurah AND ((surahNum > :minSurah AND surahNum < :maxSurah) OR (surahNum = :minSurah AND aayahNum >= :minAayah) OR (surahNum = :maxSurah AND aayahNum <= :maxAayah))) OR (surahNum = :minSurah AND surahNum = :maxSurah AND aayahNum >= :minAayah AND aayahNum <= :maxAayah)")
  void insertJuz(int juz, int minSurah, int minAayah, int maxSurah, int maxAayah);

  @Query(
      "UPDATE AayahEntity SET manzil=:manzil WHERE (:minSurah != :maxSurah AND ((surahNum > :minSurah AND surahNum < :maxSurah) OR (surahNum = :minSurah AND aayahNum >= :minAayah) OR (surahNum = :maxSurah AND aayahNum <= :maxAayah))) OR (surahNum = :minSurah AND surahNum = :maxSurah AND aayahNum >= :minAayah AND aayahNum <= :maxAayah)")
  void insertManzil(int manzil, int minSurah, int minAayah, int maxSurah, int maxAayah);

  @Query(
      "UPDATE AayahEntity SET page=:page WHERE (:minSurah != :maxSurah AND ((surahNum > :minSurah AND surahNum < :maxSurah) OR (surahNum = :minSurah AND aayahNum >= :minAayah) OR (surahNum = :maxSurah AND aayahNum <= :maxAayah))) OR (surahNum = :minSurah AND surahNum = :maxSurah AND aayahNum >= :minAayah AND aayahNum <= :maxAayah)")
  void insertPage(int page, int minSurah, int minAayah, int maxSurah, int maxAayah);

  @Query("UPDATE AayahEntity SET juzStarts=1 WHERE surahNum = :surah AND aayahNum = :aayah")
  void addJuzStarts(int surah, int aayah);

  @Query("UPDATE AayahEntity SET manzilStarts=1 WHERE surahNum = :surah AND aayahNum = :aayah")
  void addManzilStarts(int surah, int aayah);

  @Query("UPDATE AayahEntity SET rukuEnds=1 WHERE surahNum = :surah AND aayahNum = :aayah")
  void addRukuEnds(int surah, int aayah);

  @Query("UPDATE AayahEntity SET hizbEnds=1 WHERE surahNum = :surah AND aayahNum = :aayah")
  void addHizbEnds(int surah, int aayah);

  @Query("UPDATE AayahEntity SET hasSajda=1 WHERE surahNum = :surah AND aayahNum = :aayah")
  void addHasSajda(int surah, int aayah);

  @Query("SELECT * FROM AayahEntity WHERE id = :id")
  AayahEntity getAayahEntity(int id);

  @Query("SELECT * FROM AayahEntity WHERE surahNum = :surahNum AND aayahNum = :aayahNum")
  AayahEntity getAayahEntity(int surahNum, int aayahNum);

  @Query("SELECT * FROM AayahEntity WHERE id IN (:ids)")
  List<AayahEntity> getAayahEntities(List<Integer> ids);

  // Deal with SQLite host parameters limit.
  static List<AayahEntity> getAayahEntities(QuranDao dao, Set<Integer> ids) {
    List<AayahEntity> entities = new ArrayList<>();
    for (List<Integer> idList : Iterables.partition(ids, 999)) {
      entities.addAll(dao.getAayahEntities(idList));
    }
    return entities;
  }

  @Query("SELECT * FROM AayahEntity WHERE page = :page")
  List<AayahEntity> getAayahEntities(int page);

  @Query("SELECT * FROM AayahEntity WHERE manzil = :manzil AND manzilStarts")
  AayahEntity getManzilStartEntity(int manzil);

  @Query("SELECT * FROM AayahEntity WHERE juz = :juz AND juzStarts")
  AayahEntity getJuzStartEntity(int juz);

  @Query("SELECT * FROM AayahEntity WHERE surahNum = :surah AND aayahNum = 0")
  AayahEntity getSurahStartEntity(int surah);

  @Query("SELECT id FROM AayahEntity WHERE INSTR(UPPER(text), UPPER(:query)) > 0")
  List<Integer> matchQuery(String query);

  @Query("SELECT id FROM AayahEntity WHERE INSTR(UPPER(text), UPPER(:query)) = 0")
  List<Integer> misMatchQuery(String query);

  @Query("SELECT text FROM AayahEntity WHERE id = :id")
  String getTrans(int id);

  @Query("SELECT text FROM AayahEntity")
  List<String> getTexts();

  @Query("SELECT surahNum FROM AayahEntity WHERE id IN (:aayahIds)")
  List<Integer> getSurahs(List<Integer> aayahIds);
}
