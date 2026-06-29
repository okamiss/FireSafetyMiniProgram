param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('backend', 'admin', 'miniapp')]
    [string]$ServiceName,

    [Parameter(Mandatory = $true)]
    [string]$ProjectRoot
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'dev-common.ps1')

$definition = Get-DevServiceDefinitions -ProjectRoot $ProjectRoot |
    Where-Object Name -eq $ServiceName |
    Select-Object -First 1

if ($null -eq $definition) {
    throw "Unknown development service: $ServiceName"
}

foreach ($entry in $definition.Environment.GetEnumerator()) {
    Set-Item -Path ("Env:{0}" -f $entry.Key) -Value ([string]$entry.Value)
}

Set-Location -LiteralPath $definition.WorkingDirectory
Write-Host "Starting $ServiceName in $($definition.WorkingDirectory)"
Write-Host (Format-DevServiceCommand -Definition $definition)

& $definition.Command @($definition.Arguments)
exit $LASTEXITCODE
