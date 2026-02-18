$table = "accounts"
$exportDir = "exports\open_shop_data"
$outputFile = Join-Path $exportDir "$table.csv"

Write-Host "Exporting full OpenShop Table: $table"
# Use sqlcmd to export with Unicode support
sqlcmd -S "(localdb)\MSSQLLocalDB" -E -d "OpenShop_Temp" -Q "SET NOCOUNT ON; SELECT * FROM [$table]" -o $outputFile -W -u -s ","
Write-Host "Export complete: $outputFile"
