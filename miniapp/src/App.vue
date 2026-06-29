<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'
import { useSessionStore } from './stores/session'
import './styles/main.css'

const session = useSessionStore()
onLaunch(() => session.restore())
</script>
