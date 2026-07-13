package org.example.skills.enums;

public enum PriceImportTemplateType {
    /** Üreticiden gelen fiyat listesi — tedarikçiye özgü, kolon yapısı değişken */
    MANUFACTURER_LIST,
    /** CPM'den çekilen stok listesi — şirket geneli, format sabit */
    CPM_STOCK,
    /** CPM'e geri yüklenecek export — hangi alan hangi kolona/başlığa yazılacak */
    CPM_EXPORT
}
