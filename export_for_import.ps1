$targetTables = @("Categories", "items", "accounts")
$exportDir = "c:\Users\user\Library-Dr-Khaled\exports\open_shop_data"

if (!(Test-Path $exportDir)) { New-Item -ItemType Directory -Path $exportDir }

foreach ($table in $targetTables) {
    Write-Host "Exporting: $table"
    $outputFile = Join-Path $exportDir "$($table.ToLower()).csv"
    $tempCsv = [System.IO.Path]::GetTempFileName()
    
    try {
        $data = Invoke-Sqlcmd -ServerInstance "(localdb)\MSSQLLocalDB" -Database "OpenShop_Temp" -Query "SELECT * FROM [$table]" -ErrorAction Stop
        
        if ($data) {
            $data | Export-Csv -Path $tempCsv -NoTypeInformation -Encoding Unicode -Delimiter ","
            $content = Get-Content $tempCsv -Raw
            "sep=,`r`n$content" | Out-File -FilePath $outputFile -Encoding Unicode
            Write-Host "Success: $table"
        }
        else {
            Write-Host "No data: $table"
            "sep=,`r`n" | Out-File -FilePath $outputFile -Encoding Unicode
        }
    }
    catch {
        Write-Host "Error $table : $($_.Exception.Message)"
    }
    
    if (Test-Path $tempCsv) { Remove-Item $tempCsv }
}
