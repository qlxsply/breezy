package com.corwin.framework.config;

import com.corwin.framework.dict.DictEnumDefinition;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * User-selectable time zone options.
 *
 * @author Corwin 2026/5/5
 */
public enum UserTimeZoneOption implements DictEnumDefinition {
  ASIA_SHANGHAI("中国上海 Asia/Shanghai (UTC+08:00)", "Asia/Shanghai"),
  UTC("协调世界时 UTC (UTC+00:00)", "UTC"),
  ETC_GMT_PLUS_12("国际日期变更线西侧 Etc/GMT+12 (UTC-12:00)", "Etc/GMT+12"),
  PACIFIC_PAGO_PAGO("美属萨摩亚 Pago Pago (UTC-11:00)", "Pacific/Pago_Pago"),
  PACIFIC_HONOLULU("美国夏威夷 Honolulu (UTC-10:00)", "Pacific/Honolulu"),
  AMERICA_ANCHORAGE("美国阿拉斯加 Anchorage (UTC-09:00/UTC-08:00)", "America/Anchorage"),
  AMERICA_LOS_ANGELES("美国洛杉矶 Los Angeles (UTC-08:00/UTC-07:00)", "America/Los_Angeles"),
  AMERICA_DENVER("美国丹佛 Denver (UTC-07:00/UTC-06:00)", "America/Denver"),
  AMERICA_CHICAGO("美国芝加哥 Chicago (UTC-06:00/UTC-05:00)", "America/Chicago"),
  AMERICA_MEXICO_CITY("墨西哥城 Mexico City (UTC-06:00)", "America/Mexico_City"),
  AMERICA_NEW_YORK("美国纽约 New York (UTC-05:00/UTC-04:00)", "America/New_York"),
  AMERICA_BOGOTA("哥伦比亚波哥大 Bogota (UTC-05:00)", "America/Bogota"),
  AMERICA_HALIFAX("加拿大哈利法克斯 Halifax (UTC-04:00/UTC-03:00)", "America/Halifax"),
  AMERICA_CARACAS("委内瑞拉加拉加斯 Caracas (UTC-04:00)", "America/Caracas"),
  AMERICA_ST_JOHNS("加拿大纽芬兰 St. John's (UTC-03:30/UTC-02:30)", "America/St_Johns"),
  AMERICA_SAO_PAULO("巴西圣保罗 Sao Paulo (UTC-03:00)", "America/Sao_Paulo"),
  AMERICA_BUENOS_AIRES("阿根廷布宜诺斯艾利斯 Buenos Aires (UTC-03:00)", "America/Argentina/Buenos_Aires"),
  AMERICA_NORONHA("巴西费尔南多-迪诺罗尼亚 (UTC-02:00)", "America/Noronha"),
  ATLANTIC_AZORES("葡萄牙亚速尔群岛 Azores (UTC-01:00/UTC+00:00)", "Atlantic/Azores"),
  EUROPE_LONDON("英国伦敦 London (UTC+00:00/UTC+01:00)", "Europe/London"),
  EUROPE_BERLIN("德国柏林 Berlin (UTC+01:00/UTC+02:00)", "Europe/Berlin"),
  EUROPE_PARIS("法国巴黎 Paris (UTC+01:00/UTC+02:00)", "Europe/Paris"),
  AFRICA_LAGOS("尼日利亚拉各斯 Lagos (UTC+01:00)", "Africa/Lagos"),
  EUROPE_ATHENS("希腊雅典 Athens (UTC+02:00/UTC+03:00)", "Europe/Athens"),
  AFRICA_CAIRO("埃及开罗 Cairo (UTC+02:00/UTC+03:00)", "Africa/Cairo"),
  AFRICA_JOHANNESBURG("南非约翰内斯堡 Johannesburg (UTC+02:00)", "Africa/Johannesburg"),
  EUROPE_MOSCOW("俄罗斯莫斯科 Moscow (UTC+03:00)", "Europe/Moscow"),
  ASIA_RIYADH("沙特阿拉伯利雅得 Riyadh (UTC+03:00)", "Asia/Riyadh"),
  AFRICA_NAIROBI("肯尼亚内罗毕 Nairobi (UTC+03:00)", "Africa/Nairobi"),
  ASIA_TEHRAN("伊朗德黑兰 Tehran (UTC+03:30)", "Asia/Tehran"),
  ASIA_DUBAI("阿联酋迪拜 Dubai (UTC+04:00)", "Asia/Dubai"),
  ASIA_BAKU("阿塞拜疆巴库 Baku (UTC+04:00)", "Asia/Baku"),
  ASIA_KABUL("阿富汗喀布尔 Kabul (UTC+04:30)", "Asia/Kabul"),
  ASIA_KARACHI("巴基斯坦卡拉奇 Karachi (UTC+05:00)", "Asia/Karachi"),
  ASIA_TASHKENT("乌兹别克斯坦塔什干 Tashkent (UTC+05:00)", "Asia/Tashkent"),
  ASIA_KOLKATA("印度加尔各答 Kolkata (UTC+05:30)", "Asia/Kolkata"),
  ASIA_KATHMANDU("尼泊尔加德满都 Kathmandu (UTC+05:45)", "Asia/Kathmandu"),
  ASIA_DHAKA("孟加拉国达卡 Dhaka (UTC+06:00)", "Asia/Dhaka"),
  ASIA_ALMATY("哈萨克斯坦阿拉木图 Almaty (UTC+05:00)", "Asia/Almaty"),
  ASIA_YANGON("缅甸仰光 Yangon (UTC+06:30)", "Asia/Yangon"),
  ASIA_BANGKOK("泰国曼谷 Bangkok (UTC+07:00)", "Asia/Bangkok"),
  ASIA_JAKARTA("印度尼西亚雅加达 Jakarta (UTC+07:00)", "Asia/Jakarta"),
  ASIA_SINGAPORE("新加坡 Singapore (UTC+08:00)", "Asia/Singapore"),
  AUSTRALIA_PERTH("澳大利亚珀斯 Perth (UTC+08:00)", "Australia/Perth"),
  AUSTRALIA_EUCLA("澳大利亚尤克拉 Eucla (UTC+08:45)", "Australia/Eucla"),
  ASIA_TOKYO("日本东京 Tokyo (UTC+09:00)", "Asia/Tokyo"),
  ASIA_SEOUL("韩国首尔 Seoul (UTC+09:00)", "Asia/Seoul"),
  AUSTRALIA_ADELAIDE("澳大利亚阿德莱德 Adelaide (UTC+09:30/UTC+10:30)", "Australia/Adelaide"),
  AUSTRALIA_SYDNEY("澳大利亚悉尼 Sydney (UTC+10:00/UTC+11:00)", "Australia/Sydney"),
  AUSTRALIA_BRISBANE("澳大利亚布里斯班 Brisbane (UTC+10:00)", "Australia/Brisbane"),
  AUSTRALIA_LORD_HOWE("澳大利亚豪勋爵岛 Lord Howe (UTC+10:30/UTC+11:00)", "Australia/Lord_Howe"),
  PACIFIC_NOUMEA("新喀里多尼亚 Noumea (UTC+11:00)", "Pacific/Noumea"),
  PACIFIC_GUADALCANAL("所罗门群岛 Guadalcanal (UTC+11:00)", "Pacific/Guadalcanal"),
  PACIFIC_AUCKLAND("新西兰奥克兰 Auckland (UTC+12:00/UTC+13:00)", "Pacific/Auckland"),
  PACIFIC_FIJI("斐济 Fiji (UTC+12:00)", "Pacific/Fiji"),
  PACIFIC_CHATHAM("新西兰查塔姆群岛 Chatham (UTC+12:45/UTC+13:45)", "Pacific/Chatham"),
  PACIFIC_APIA("萨摩亚阿皮亚 Apia (UTC+13:00)", "Pacific/Apia"),
  PACIFIC_TONGATAPU("汤加 Tongatapu (UTC+13:00)", "Pacific/Tongatapu"),
  PACIFIC_KIRITIMATI("基里巴斯圣诞岛 Kiritimati (UTC+14:00)", "Pacific/Kiritimati");

  private final String label;
  private final String zoneId;

  UserTimeZoneOption(String label, String zoneId) {
    this.label = label;
    this.zoneId = zoneId;
  }

  @Override
  public String label() {
    return label;
  }

  @Override
  @JsonValue
  public String itemValue() {
    return zoneId;
  }

  public String zoneId() {
    return zoneId;
  }

  @JsonCreator
  public static UserTimeZoneOption fromCode(String code) {
    if (code == null || code.isBlank()) {
      return ASIA_SHANGHAI;
    }
    String normalized = code.trim();
    for (UserTimeZoneOption option : values()) {
      if (option.itemValue().equals(normalized)) {
        return option;
      }
    }
    return ASIA_SHANGHAI;
  }

  public static String zoneIdOf(String code) {
    return fromCode(code).zoneId();
  }
}
